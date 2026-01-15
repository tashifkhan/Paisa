from fastapi import APIRouter, Depends, Query
from datetime import datetime
from typing import Optional, List
from pydantic import BaseModel

from ..core.deps import get_current_user
from ..models import db_models as models

router = APIRouter(prefix="/sync", tags=["sync"])


class SyncRequest(BaseModel):
    """Request body for sync endpoint."""

    last_synced_at: Optional[datetime] = None
    # Optional: incoming changes from client (for two-way sync)
    transactions: Optional[List[dict]] = None
    wallets: Optional[List[dict]] = None
    categories: Optional[List[dict]] = None


class SyncResponse(BaseModel):
    """Response from sync endpoint."""

    synced_at: datetime
    transactions: List[dict]
    wallets: List[dict]
    categories: List[dict]
    groups: List[dict]
    debts: List[dict]


@router.post("/", response_model=SyncResponse)
async def sync_data(
    payload: SyncRequest,
    current_user=Depends(get_current_user),
):
    """
    Sync data between client and server.

    - If last_synced_at is provided, returns only entities modified after that time
    - If last_synced_at is None, returns all entities
    - Updates user's last_synced_at timestamp
    """
    last_sync = payload.last_synced_at
    now = datetime.utcnow()

    # Build query filter
    def get_filter(model):
        base = [model.user_id == current_user.id, model.deleted == False]
        if last_sync:
            base.append(model.updated_at > last_sync)
        return base

    # Fetch transactions
    txns = await models.Transaction.find(*get_filter(models.Transaction)).to_list()

    # Fetch wallets
    wallets = await models.Wallet.find(*get_filter(models.Wallet)).to_list()

    # Fetch user's custom categories
    cat_filter = [
        models.Category.user_id == current_user.id,
        models.Category.deleted == False,
    ]
    if last_sync:
        cat_filter.append(models.Category.created_at > last_sync)
    categories = await models.Category.find(*cat_filter).to_list()

    # Also include default categories on first sync
    if not last_sync:
        default_cats = await models.Category.find(
            models.Category.is_default == True,
            models.Category.deleted == False,
        ).to_list()
        categories.extend(default_cats)

    # Fetch groups user is member of
    memberships = await models.GroupMember.find(
        models.GroupMember.user_id == current_user.id,
        models.GroupMember.deleted == False,
    ).to_list()
    group_ids = [m.group_id for m in memberships]

    groups = []
    if group_ids:
        group_filter = [{"id": {"$in": group_ids}}, models.Group.deleted == False]
        if last_sync:
            group_filter.append(models.Group.updated_at > last_sync)
        groups = await models.Group.find(*group_filter).to_list()

    # Fetch debts
    debts = await models.Debt.find(*get_filter(models.Debt)).to_list()

    # Update user's last synced timestamp
    current_user.last_synced_at = now
    await current_user.save()

    # Serialize results
    def serialize(items):
        result = []
        for item in items:
            d = item.model_dump()
            # Convert UUIDs to strings
            for key, val in d.items():
                if hasattr(val, "hex"):  # UUID
                    d[key] = str(val)
                elif isinstance(val, datetime):
                    d[key] = val.isoformat()
            result.append(d)
        return result

    return SyncResponse(
        synced_at=now,
        transactions=serialize(txns),
        wallets=serialize(wallets),
        categories=serialize(categories),
        groups=serialize(groups),
        debts=serialize(debts),
    )


@router.get("/status")
async def sync_status(current_user=Depends(get_current_user)):
    """Get current sync status for the user."""
    return {
        "user_id": str(current_user.id),
        "last_synced_at": (
            current_user.last_synced_at.isoformat()
            if current_user.last_synced_at
            else None
        ),
        "server_time": datetime.utcnow().isoformat(),
    }


@router.get("/counts")
async def sync_counts(
    current_user=Depends(get_current_user),
    since: Optional[datetime] = Query(
        None, description="Count changes since this time"
    ),
):
    """Get counts of entities that need syncing."""

    def get_filter(model):
        base = [model.user_id == current_user.id]
        if since:
            base.append(model.updated_at > since)
        return base

    txn_count = await models.Transaction.find(*get_filter(models.Transaction)).count()
    wallet_count = await models.Wallet.find(*get_filter(models.Wallet)).count()
    debt_count = await models.Debt.find(*get_filter(models.Debt)).count()

    # Categories
    cat_filter = [models.Category.user_id == current_user.id]
    if since:
        cat_filter.append(models.Category.created_at > since)
    cat_count = await models.Category.find(*cat_filter).count()

    return {
        "since": since.isoformat() if since else None,
        "counts": {
            "transactions": txn_count,
            "wallets": wallet_count,
            "categories": cat_count,
            "debts": debt_count,
        },
    }
