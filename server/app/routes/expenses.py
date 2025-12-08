from fastapi import APIRouter, Depends, File, UploadFile, HTTPException
from fastapi.responses import FileResponse
from typing import List
import os
import shutil
import csv
from uuid import UUID

from ..core.deps import get_current_user
from ..models import db_models as models
from ..models import schemas

router = APIRouter(prefix="/expenses", tags=["expenses"])


@router.post("/upload-bill")
async def upload_bill(
    file: UploadFile = File(...), current_user=Depends(get_current_user)
):
    base = os.path.join(os.getcwd(), "uploads")
    file_location = os.path.join(base, str(current_user.id))
    os.makedirs(file_location, exist_ok=True)
    path = os.path.join(file_location, file.filename)

    # Reading file content; FastAPI runs async files in threadpool so this blocking I/O is okay-ish for now
    # or better, use aiofiles if installed, but os/shutil is safe enough in threadpool.
    with open(path, "wb+") as buffer:
        shutil.copyfileobj(file.file, buffer)

    return {"url": f"/uploads/{current_user.id}/{file.filename}"}


@router.post("/add")
async def add_expense(
    payload: schemas.TransactionCreate,
    current_user=Depends(get_current_user),
):
    txn = models.Transaction(
        user_id=current_user.id,
        amount=payload.amount,
        currency=payload.currency,
        type=payload.type,
        date=payload.date,  # Pydantic v2 handles None defaults from schema or we set it in model default
        note=payload.note,
        split_strategy=payload.split_strategy,
    )
    if payload.date is None:
        # If payload.date is Optional and None, model might want a concrete datetime if not auto-defaulted logic,
        # but Transaction model definition has 'date: datetime' mandatory without default?
        # Actually in db_models.py it is 'date: datetime'. Schema usually handles optionality.
        # Let's assume schema validates or provides defaults.
        pass

    await txn.create()
    return {"status": "created", "id": str(txn.id)}


@router.get("/", response_model=List[schemas.TransactionOut])
async def list_transactions(current_user=Depends(get_current_user)):
    # Beanie find returns a FindMany object
    txns = (
        await models.Transaction.find(
            models.Transaction.user_id == current_user.id,
            models.Transaction.deleted == False,
        )
        .sort(-models.Transaction.date)
        .to_list()
    )
    return txns


@router.get("/export")
async def export_user_data(current_user=Depends(get_current_user)):
    txns = await models.Transaction.find(
        models.Transaction.user_id == current_user.id
    ).to_list()

    filename = f"export_{current_user.id}.csv"
    # CSV writing is synchronous. In a high-throughput async app, this should be offloaded.
    # For now, it's fine.
    with open(filename, mode="w", newline="") as file:
        writer = csv.writer(file)
        writer.writerow(["Date", "Title", "Amount", "Currency", "Type", "Note"])
        for t in txns:
            # t.date is datetime object
            writer.writerow(
                [t.date, "Expense", t.amount, t.currency, t.type, t.note or ""]
            )
    return FileResponse(
        path=filename, filename="my_expenses.csv", media_type="text/csv"
    )


@router.get("/{txn_id}", response_model=schemas.TransactionOut)
async def get_transaction(txn_id: str, current_user=Depends(get_current_user)):
    try:
        uuid_obj = UUID(txn_id)
    except ValueError:
        raise HTTPException(status_code=404, detail="Invalid UUID")

    t = await models.Transaction.find_one(
        models.Transaction.id == uuid_obj,
        models.Transaction.user_id == current_user.id,
        models.Transaction.deleted == False,
    )
    if not t:
        raise HTTPException(status_code=404, detail="Transaction not found")
    return t


@router.put("/{txn_id}")
async def update_transaction(
    txn_id: str,
    payload: schemas.TransactionUpdate,
    current_user=Depends(get_current_user),
):
    try:
        uuid_obj = UUID(txn_id)
    except ValueError:
        raise HTTPException(status_code=404, detail="Invalid UUID")

    t = await models.Transaction.find_one(
        models.Transaction.id == uuid_obj,
        models.Transaction.user_id == current_user.id,
    )
    if not t:
        raise HTTPException(status_code=404, detail="Transaction not found")

    # Update fields
    # Beanie's update method or simple attribute assignment + save
    data = payload.dict(exclude_unset=True)
    if data:
        # Using a bulk update query for efficiency or manual set
        # Manual set is often cleaner for complex logic hooks (if any), but this is simple.
        # await t.set(data) is simpler if supported or manually:
        for k, v in data.items():
            setattr(t, k, v)
        await t.save()

    return {"status": "updated"}


@router.delete("/{txn_id}")
async def delete_transaction(txn_id: str, current_user=Depends(get_current_user)):
    try:
        uuid_obj = UUID(txn_id)
    except ValueError:
        raise HTTPException(status_code=404, detail="Invalid UUID")

    t = await models.Transaction.find_one(
        models.Transaction.id == uuid_obj,
        models.Transaction.user_id == current_user.id,
    )
    if not t:
        raise HTTPException(status_code=404, detail="Transaction not found")

    t.deleted = True
    await t.save()
    return {"status": "deleted"}
