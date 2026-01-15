from fastapi import APIRouter, Depends, HTTPException
from typing import List
from uuid import UUID
from datetime import datetime

from ..core.deps import get_current_user
from ..models import db_models as models
from ..models import schemas
from ..services.wallet_service import WalletService

router = APIRouter(prefix="/wallets", tags=["wallets"])


@router.get("/", response_model=List[schemas.WalletOut])
async def list_wallets(current_user=Depends(get_current_user)):
    """List all wallets for the current user."""
    wallets = await WalletService.get_wallets(current_user.id)
    return wallets


@router.get("/total")
async def get_total_balance(
    current_user=Depends(get_current_user),
    currency: str = "INR",
):
    """Get total balance across all wallets in a specific currency."""
    total = await WalletService.get_total_balance(current_user.id, currency)
    return {
        "currency": currency,
        "total_balance": total,
    }


@router.post("/", response_model=dict)
async def create_wallet(
    payload: schemas.WalletCreate,
    current_user=Depends(get_current_user),
):
    """Create a new wallet."""
    w = await WalletService.create_wallet(
        user_id=current_user.id,
        name=payload.name,
        wallet_type=payload.type,
        currency=payload.currency,
    )
    return {
        "status": "created",
        "id": str(w.id),
    }


@router.get("/{wallet_id}", response_model=schemas.WalletOut)
async def get_wallet(wallet_id: str, current_user=Depends(get_current_user)):
    """Get a single wallet by ID."""
    try:
        uuid_obj = UUID(wallet_id)
    except ValueError:
        raise HTTPException(status_code=404, detail="Invalid UUID")

    w = await WalletService.get_wallet(uuid_obj, current_user.id)
    if not w:
        raise HTTPException(status_code=404, detail="Wallet not found")

    return w


@router.put("/{wallet_id}", response_model=schemas.WalletOut)
async def update_wallet(
    wallet_id: str,
    payload: schemas.WalletUpdate,
    current_user=Depends(get_current_user),
):
    """Update a wallet."""
    try:
        uuid_obj = UUID(wallet_id)
    except ValueError:
        raise HTTPException(status_code=404, detail="Invalid UUID")

    w = await WalletService.get_wallet(uuid_obj, current_user.id)
    if not w:
        raise HTTPException(status_code=404, detail="Wallet not found")

    w = await WalletService.update_wallet(
        w,
        name=payload.name,
        wallet_type=payload.type,
        currency=payload.currency,
    )
    return w


@router.delete("/{wallet_id}", response_model=dict)
async def delete_wallet(
    wallet_id: str,
    current_user=Depends(get_current_user),
):
    """Delete a wallet (soft delete)."""
    try:
        uuid_obj = UUID(wallet_id)
    except ValueError:
        raise HTTPException(status_code=404, detail="Invalid UUID")

    w = await WalletService.get_wallet(uuid_obj, current_user.id)
    if not w:
        raise HTTPException(status_code=404, detail="Wallet not found")

    await WalletService.delete_wallet(w)
    return {"status": "deleted"}


@router.get("/{wallet_id}/transactions", response_model=List[schemas.TransactionOut])
async def get_wallet_transactions(
    wallet_id: str,
    current_user=Depends(get_current_user),
):
    """Get all transactions for a specific wallet."""
    try:
        uuid_obj = UUID(wallet_id)
    except ValueError:
        raise HTTPException(status_code=404, detail="Invalid UUID")

    # Verify wallet exists and belongs to user
    w = await WalletService.get_wallet(uuid_obj, current_user.id)
    if not w:
        raise HTTPException(status_code=404, detail="Wallet not found")

    txns = (
        await models.Transaction.find(
            models.Transaction.wallet_id == uuid_obj,
            models.Transaction.deleted == False,
        )
        .sort(-models.Transaction.date)
        .to_list()
    )
    return txns


@router.post("/{wallet_id}/adjust-balance")
async def adjust_wallet_balance(
    wallet_id: str,
    amount: float,
    current_user=Depends(get_current_user),
):
    """
    Manually adjust wallet balance.
    Use positive amount to add, negative to subtract.
    """
    try:
        uuid_obj = UUID(wallet_id)
    except ValueError:
        raise HTTPException(status_code=404, detail="Invalid UUID")

    w = await WalletService.get_wallet(uuid_obj, current_user.id)
    if not w:
        raise HTTPException(status_code=404, detail="Wallet not found")

    old_balance = w.balance
    await WalletService.adjust_balance(w, amount)

    return {
        "status": "adjusted",
        "old_balance": old_balance,
        "new_balance": w.balance,
        "adjustment": amount,
    }
