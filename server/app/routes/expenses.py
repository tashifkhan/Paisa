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


def _reverse_wallet_effect(wallet: models.Wallet, amount: float, txn_type: str) -> None:
    is_credit = WalletService._is_credit_wallet(wallet.type)
    if txn_type == "income":
        wallet.balance = (
            wallet.balance + amount if is_credit else wallet.balance - amount
        )
    elif txn_type == "expense":
        wallet.balance = (
            wallet.balance - amount if is_credit else wallet.balance + amount
        )


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
    to_wallet_uuid = None
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

    if payload.to_wallet_id:
        try:
            to_wallet_uuid = UUID(payload.to_wallet_id)
        except ValueError:
            raise HTTPException(status_code=400, detail="Invalid to_wallet_id UUID")

    if payload.type == "transfer":
        if not wallet_uuid or not to_wallet_uuid:
            raise HTTPException(
                status_code=400,
                detail="transfer requires wallet_id and to_wallet_id",
            )
        if wallet_uuid == to_wallet_uuid:
            raise HTTPException(
                status_code=400,
                detail="transfer source and destination cannot be the same",
            )

    # Build embedded splits
    embedded_splits = []
    if payload.splits:
        for s in payload.splits:
            try:
                split_user_uuid = UUID(s.user_id)
            except ValueError:
                raise HTTPException(
                    status_code=400, detail=f"Invalid user_id UUID in split: {s.user_id}"
                )
            embedded_splits.append(
                models.TransactionSplit(
                    user_id=split_user_uuid,
                    amount_owed=s.amount_owed or 0.0,
                    percentage=s.percentage,
                    shares=s.shares,
                )
            )

    txn = models.Transaction(
        user_id=current_user.id,
        amount=payload.amount,
        currency=payload.currency,
        type=payload.type,
        date=payload.date or datetime.utcnow(),
        note=payload.note,
        title=payload.title,
        description=payload.description,
        split_strategy=payload.split_strategy,
        bill_image_url=payload.bill_image_url,
        wallet_id=wallet_uuid,
        to_wallet_id=to_wallet_uuid,
        group_id=group_uuid,
        category_id=category_uuid,
        splits=embedded_splits,
    )

    # Validate wallets exist and belong to user
    source_wallet = None
    destination_wallet = None
    if wallet_uuid:
        source_wallet = await WalletService.get_wallet(wallet_uuid, current_user.id)
        if not source_wallet:
            raise HTTPException(status_code=400, detail="source wallet not found")

    if to_wallet_uuid:
        destination_wallet = await WalletService.get_wallet(
            to_wallet_uuid, current_user.id
        )
        if not destination_wallet:
            raise HTTPException(status_code=400, detail="destination wallet not found")

    # Apply wallet effects first, persist txn only if valid
    try:
        if payload.type == "transfer":
            if source_wallet:
                await WalletService.update_balance(
                    source_wallet, payload.amount, "expense"
                )
            if destination_wallet:
                await WalletService.update_balance(
                    destination_wallet, payload.amount, "income"
                )
        elif source_wallet:
            await WalletService.update_balance(
                source_wallet, payload.amount, payload.type
            )
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc

    await txn.create()

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
    old_to_wallet_id = t.to_wallet_id

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

    if "to_wallet_id" in data and data["to_wallet_id"]:
        try:
            data["to_wallet_id"] = UUID(data["to_wallet_id"])
        except ValueError:
            raise HTTPException(status_code=400, detail="Invalid to_wallet_id UUID")

    final_type = data.get("type", t.type)
    final_wallet_id = data.get("wallet_id", t.wallet_id)
    final_to_wallet_id = data.get("to_wallet_id", t.to_wallet_id)
    final_amount = data.get("amount", t.amount)

    if final_type == "transfer":
        if not final_wallet_id or not final_to_wallet_id:
            raise HTTPException(
                status_code=400,
                detail="transfer requires wallet_id and to_wallet_id",
            )
        if final_wallet_id == final_to_wallet_id:
            raise HTTPException(
                status_code=400,
                detail="transfer source and destination cannot be the same",
            )

    if data:
        # Validate target wallets exist before any balance mutation
        new_source_wallet = None
        new_destination_wallet = None
        if final_wallet_id:
            new_source_wallet = await WalletService.get_wallet(
                final_wallet_id, current_user.id
            )
            if not new_source_wallet:
                raise HTTPException(status_code=400, detail="source wallet not found")
        if final_to_wallet_id:
            new_destination_wallet = await WalletService.get_wallet(
                final_to_wallet_id, current_user.id
            )
            if not new_destination_wallet:
                raise HTTPException(
                    status_code=400, detail="destination wallet not found"
                )

        # Reverse previous wallet effects
        old_source_wallet = None
        old_destination_wallet = None
        if old_wallet_id:
            old_source_wallet = await WalletService.get_wallet(
                old_wallet_id, current_user.id
            )
        if old_to_wallet_id:
            old_destination_wallet = await WalletService.get_wallet(
                old_to_wallet_id, current_user.id
            )

        if old_type == "transfer":
            if old_source_wallet:
                _reverse_wallet_effect(old_source_wallet, old_amount, "expense")
                await old_source_wallet.save()
            if old_destination_wallet:
                _reverse_wallet_effect(old_destination_wallet, old_amount, "income")
                await old_destination_wallet.save()
        else:
            if old_source_wallet:
                _reverse_wallet_effect(old_source_wallet, old_amount, old_type)
                await old_source_wallet.save()

        try:
            if final_type == "transfer":
                if new_source_wallet:
                    await WalletService.update_balance(
                        new_source_wallet, final_amount, "expense"
                    )
                if new_destination_wallet:
                    await WalletService.update_balance(
                        new_destination_wallet, final_amount, "income"
                    )
            elif new_source_wallet:
                await WalletService.update_balance(
                    new_source_wallet, final_amount, final_type
                )
        except ValueError as exc:
            # Roll back reversal to preserve previous state
            try:
                if old_type == "transfer":
                    if old_source_wallet:
                        await WalletService.update_balance(
                            old_source_wallet, old_amount, "expense"
                        )
                    if old_destination_wallet:
                        await WalletService.update_balance(
                            old_destination_wallet, old_amount, "income"
                        )
                elif old_source_wallet:
                    await WalletService.update_balance(
                        old_source_wallet, old_amount, old_type
                    )
            except Exception:
                pass
            raise HTTPException(status_code=400, detail=str(exc)) from exc

        # Convert splits from schema objects to embedded models before applying
        if "splits" in data and data["splits"] is not None:
            embedded_splits = []
            for s in data["splits"]:
                try:
                    split_user_uuid = UUID(s.user_id if hasattr(s, 'user_id') else s['user_id'])
                except (ValueError, KeyError):
                    raise HTTPException(
                        status_code=400, detail="Invalid user_id UUID in split"
                    )
                amount_owed = s.amount_owed if hasattr(s, 'amount_owed') else s.get('amount_owed', 0.0)
                percentage = s.percentage if hasattr(s, 'percentage') else s.get('percentage')
                shares = s.shares if hasattr(s, 'shares') else s.get('shares')
                embedded_splits.append(
                    models.TransactionSplit(
                        user_id=split_user_uuid,
                        amount_owed=amount_owed or 0.0,
                        percentage=percentage,
                        shares=shares,
                    )
                )
            data["splits"] = embedded_splits

        for k, v in data.items():
            setattr(t, k, v)
        t.updated_at = datetime.utcnow()
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

    # Reverse wallet balance if applicable
    if t.type == "transfer":
        if t.wallet_id:
            source_wallet = await WalletService.get_wallet(t.wallet_id, current_user.id)
            if source_wallet:
                _reverse_wallet_effect(source_wallet, t.amount, "expense")
                await source_wallet.save()
        if t.to_wallet_id:
            destination_wallet = await WalletService.get_wallet(
                t.to_wallet_id, current_user.id
            )
            if destination_wallet:
                _reverse_wallet_effect(destination_wallet, t.amount, "income")
                await destination_wallet.save()
    elif t.wallet_id:
        wallet = await WalletService.get_wallet(t.wallet_id, current_user.id)
        if wallet:
            _reverse_wallet_effect(wallet, t.amount, t.type)
            await wallet.save()

    t.deleted = True
    t.updated_at = datetime.utcnow()
    await t.save()
    return {"status": "deleted"}
