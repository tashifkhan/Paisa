from fastapi import APIRouter, Depends, HTTPException
from typing import List
from uuid import UUID

from ..core.deps import get_current_user
from ..models import db_models as models
from ..models import schemas

router = APIRouter(prefix="/wallets", tags=["wallets"])


@router.get("/", response_model=List[schemas.WalletOut])
async def list_wallets(current_user=Depends(get_current_user)):
    wallets = await models.Wallet.find(
        models.Wallet.user_id == current_user.id, models.Wallet.deleted == False
    ).to_list()
    return wallets


@router.post("/", response_model=dict)
async def create_wallet(
    payload: schemas.WalletCreate,
    current_user=Depends(get_current_user),
):
    w = models.Wallet(
        user_id=current_user.id,
        name=payload.name,
        type=payload.type,
        currency=payload.currency,
    )
    await w.create()
    return {
        "status": "created",
        "id": str(w.id),
    }


@router.put("/{wallet_id}", response_model=dict)
async def update_wallet(
    wallet_id: str,
    payload: schemas.WalletCreate,
    current_user=Depends(get_current_user),
):
    try:
        uuid_obj = UUID(wallet_id)
    except ValueError:
        raise HTTPException(status_code=404, detail="Invalid UUID")

    w = await models.Wallet.find_one(
        models.Wallet.id == uuid_obj, models.Wallet.user_id == current_user.id
    )

    if not w:
        raise HTTPException(
            status_code=404,
            detail="Wallet not found",
        )
    w.name = payload.name
    w.type = payload.type
    w.currency = payload.currency
    await w.save()

    return {
        "status": "updated",
    }


@router.delete("/{wallet_id}", response_model=dict)
async def delete_wallet(
    wallet_id: str,
    current_user=Depends(get_current_user),
):
    try:
        uuid_obj = UUID(wallet_id)
    except ValueError:
        raise HTTPException(status_code=404, detail="Invalid UUID")

    w = await models.Wallet.find_one(
        models.Wallet.id == uuid_obj, models.Wallet.user_id == current_user.id
    )
    if not w:
        raise HTTPException(
            status_code=404,
            detail="Wallet not found",
        )
    w.deleted = True
    await w.save()
    return {
        "status": "deleted",
    }
