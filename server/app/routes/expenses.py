from fastapi import APIRouter, Depends, File, UploadFile, HTTPException, Query
from fastapi.responses import FileResponse
from typing import List, Optional
import os
import shutil
import csv
from uuid import UUID
from datetime import datetime

from ..core.deps import get_current_user
from ..models import db_models as models
from ..models import schemas
from ..services.wallet_service import WalletService

router = APIRouter(prefix="/expenses", tags=["expenses"])


@router.post("/upload-bill")
async def upload_bill(
    file: UploadFile = File(...), current_user=Depends(get_current_user)
):
    base = os.path.join(os.getcwd(), "uploads")
    file_location = os.path.join(base, str(current_user.id))
    os.makedirs(file_location, exist_ok=True)
    path = os.path.join(file_location, file.filename)

    with open(path, "wb+") as buffer:
        shutil.copyfileobj(file.file, buffer)

    return {"url": f"/uploads/{current_user.id}/{file.filename}"}


@router.post("/add")
async def add_expense(
    payload: schemas.TransactionCreate,
    current_user=Depends(get_current_user),
):
    # Parse optional UUIDs
    wallet_uuid = None
    group_uuid = None
    category_uuid = None

    if payload.wallet_id:
        try:
            wallet_uuid = UUID(payload.wallet_id)
        except ValueError:
            raise HTTPException(status_code=400, detail="Invalid wallet_id UUID")

    if payload.group_id:
        try:
            group_uuid = UUID(payload.group_id)
        except ValueError:
            raise HTTPException(status_code=400, detail="Invalid group_id UUID")

    if payload.category_id:
        try:
            category_uuid = UUID(payload.category_id)
        except ValueError:
            raise HTTPException(status_code=400, detail="Invalid category_id UUID")

    txn = models.Transaction(
        user_id=current_user.id,
        amount=payload.amount,
        currency=payload.currency,
        type=payload.type,
        date=payload.date or datetime.utcnow(),
        note=payload.note,
        split_strategy=payload.split_strategy,
        wallet_id=wallet_uuid,
        group_id=group_uuid,
        category_id=category_uuid,
    )

    await txn.create()

    # Update wallet balance if wallet is specified
    if wallet_uuid:
        wallet = await WalletService.get_wallet(wallet_uuid, current_user.id)
        if wallet:
            await WalletService.update_balance(wallet, payload.amount, payload.type)

    return {"status": "created", "id": str(txn.id)}


@router.get("/", response_model=List[schemas.TransactionOut])
async def list_transactions(
    current_user=Depends(get_current_user),
    wallet_id: Optional[str] = Query(None, description="Filter by wallet"),
    category_id: Optional[str] = Query(None, description="Filter by category"),
    type: Optional[str] = Query(None, description="Filter by type (expense/income)"),
    start_date: Optional[datetime] = Query(None, description="Filter from date"),
    end_date: Optional[datetime] = Query(None, description="Filter to date"),
    limit: int = Query(100, le=500, description="Max results"),
):
    """List transactions with optional filters."""
    query_conditions = [
        models.Transaction.user_id == current_user.id,
        models.Transaction.deleted == False,
    ]

    if wallet_id:
        try:
            query_conditions.append(models.Transaction.wallet_id == UUID(wallet_id))
        except ValueError:
            raise HTTPException(status_code=400, detail="Invalid wallet_id UUID")

    if category_id:
        try:
            query_conditions.append(models.Transaction.category_id == UUID(category_id))
        except ValueError:
            raise HTTPException(status_code=400, detail="Invalid category_id UUID")

    if type:
        query_conditions.append(models.Transaction.type == type)

    if start_date:
        query_conditions.append(models.Transaction.date >= start_date)

    if end_date:
        query_conditions.append(models.Transaction.date <= end_date)

    txns = (
        await models.Transaction.find(*query_conditions)
        .sort(-models.Transaction.date)
        .limit(limit)
        .to_list()
    )
    return txns


@router.get("/export")
async def export_user_data(current_user=Depends(get_current_user)):
    txns = await models.Transaction.find(
        models.Transaction.user_id == current_user.id
    ).to_list()

    filename = f"export_{current_user.id}.csv"
    with open(filename, mode="w", newline="") as file:
        writer = csv.writer(file)
        writer.writerow(
            [
                "Date",
                "Title",
                "Amount",
                "Currency",
                "Type",
                "Category",
                "Wallet",
                "Note",
            ]
        )
        for t in txns:
            # Get category name if exists
            cat_name = ""
            if t.category_id:
                cat = await models.Category.get(t.category_id)
                if cat:
                    cat_name = cat.name

            # Get wallet name if exists
            wallet_name = ""
            if t.wallet_id:
                wallet = await models.Wallet.get(t.wallet_id)
                if wallet:
                    wallet_name = wallet.name

            writer.writerow(
                [
                    t.date,
                    "Transaction",
                    t.amount,
                    t.currency,
                    t.type,
                    cat_name,
                    wallet_name,
                    t.note or "",
                ]
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

    old_amount = t.amount
    old_type = t.type
    old_wallet_id = t.wallet_id

    # Update fields
    data = payload.model_dump(exclude_unset=True)

    # Handle UUID fields
    if "wallet_id" in data and data["wallet_id"]:
        try:
            data["wallet_id"] = UUID(data["wallet_id"])
        except ValueError:
            raise HTTPException(status_code=400, detail="Invalid wallet_id UUID")

    if "category_id" in data and data["category_id"]:
        try:
            data["category_id"] = UUID(data["category_id"])
        except ValueError:
            raise HTTPException(status_code=400, detail="Invalid category_id UUID")

    if data:
        for k, v in data.items():
            setattr(t, k, v)
        t.updated_at = datetime.utcnow()
        await t.save()

        new_amount = t.amount
        new_type = t.type
        new_wallet_id = t.wallet_id

        # Adjust wallet balances if amount, type, or wallet changed
        if old_wallet_id and (
            old_amount != new_amount
            or old_type != new_type
            or old_wallet_id != new_wallet_id
        ):
            # Reverse old transaction effect
            old_wallet = await WalletService.get_wallet(old_wallet_id, current_user.id)
            if old_wallet:
                if old_type == "income":
                    old_wallet.balance -= old_amount
                elif old_type == "expense":
                    old_wallet.balance += old_amount
                await old_wallet.save()

        if new_wallet_id:
            # Apply new transaction effect
            new_wallet = await WalletService.get_wallet(new_wallet_id, current_user.id)
            if new_wallet:
                await WalletService.update_balance(new_wallet, new_amount, new_type)

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

    # Reverse wallet balance if applicable
    if t.wallet_id:
        wallet = await WalletService.get_wallet(t.wallet_id, current_user.id)
        if wallet:
            if t.type == "income":
                wallet.balance -= t.amount
            elif t.type == "expense":
                wallet.balance += t.amount
            await wallet.save()

    t.deleted = True
    t.updated_at = datetime.utcnow()
    await t.save()
    return {"status": "deleted"}
