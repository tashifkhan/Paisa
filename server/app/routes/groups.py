from fastapi import APIRouter, Depends, HTTPException
from typing import List
from uuid import UUID

from ..core.deps import get_current_user
from ..models import db_models as models
from ..models import schemas
from ..services.group_service import GroupService

router = APIRouter(prefix="/groups", tags=["groups"])


@router.post("/", response_model=dict)
async def create_group(
    payload: schemas.GroupCreate,
    current_user=Depends(get_current_user),
):
    """Create a new group. Creator is automatically added as admin."""
    g = await GroupService.create_group(
        name=payload.name,
        created_by_user_id=current_user.id,
        base_currency=payload.base_currency,
        icon=payload.icon,
        color=payload.color,
    )
    return {
        "status": "created",
        "id": str(g.id),
    }


@router.get("/", response_model=List[schemas.GroupOut])
async def list_groups(current_user=Depends(get_current_user)):
    """List groups the current user is a member of."""
    groups = await GroupService.get_groups(current_user.id)
    return groups


@router.get("/{group_id}", response_model=schemas.GroupOut)
async def get_group(group_id: str, current_user=Depends(get_current_user)):
    """Get a single group by ID."""
    try:
        uuid_obj = UUID(group_id)
    except ValueError:
        raise HTTPException(status_code=404, detail="Invalid UUID")

    # Check membership
    if not await GroupService.is_member(uuid_obj, current_user.id):
        raise HTTPException(status_code=403, detail="Not a member of this group")

    g = await GroupService.get_group(uuid_obj)
    if not g:
        raise HTTPException(status_code=404, detail="Group not found")

    return g


@router.put("/{group_id}", response_model=schemas.GroupOut)
async def update_group(
    group_id: str,
    payload: schemas.GroupUpdate,
    current_user=Depends(get_current_user),
):
    """Update a group. Only admins can update."""
    try:
        uuid_obj = UUID(group_id)
    except ValueError:
        raise HTTPException(status_code=404, detail="Invalid UUID")

    # Check admin permission
    if not await GroupService.is_admin(uuid_obj, current_user.id):
        raise HTTPException(status_code=403, detail="Only admins can update the group")

    g = await GroupService.get_group(uuid_obj)
    if not g:
        raise HTTPException(status_code=404, detail="Group not found")

    g = await GroupService.update_group(
        g,
        name=payload.name,
        base_currency=payload.base_currency,
        icon=payload.icon,
        color=payload.color,
    )
    return g


@router.delete("/{group_id}", response_model=dict)
async def delete_group(group_id: str, current_user=Depends(get_current_user)):
    """Delete a group. Only admins can delete."""
    try:
        uuid_obj = UUID(group_id)
    except ValueError:
        raise HTTPException(status_code=404, detail="Invalid UUID")

    # Check admin permission
    if not await GroupService.is_admin(uuid_obj, current_user.id):
        raise HTTPException(status_code=403, detail="Only admins can delete the group")

    g = await GroupService.get_group(uuid_obj)
    if not g:
        raise HTTPException(status_code=404, detail="Group not found")

    await GroupService.delete_group(g)
    return {"status": "deleted"}


# Member Management Endpoints
@router.get("/{group_id}/members", response_model=List[schemas.GroupMemberWithUser])
async def get_group_members(group_id: str, current_user=Depends(get_current_user)):
    """Get all members of a group with user details."""
    try:
        uuid_obj = UUID(group_id)
    except ValueError:
        raise HTTPException(status_code=404, detail="Invalid UUID")

    # Check membership
    if not await GroupService.is_member(uuid_obj, current_user.id):
        raise HTTPException(status_code=403, detail="Not a member of this group")

    members = await GroupService.get_members(uuid_obj)

    # Enrich with user data
    result = []
    for m in members:
        user = await models.User.get(m.user_id)
        result.append(
            {
                "id": str(m.id),
                "user_id": str(m.user_id),
                "role": m.role,
                "joined_at": m.joined_at,
                "user_name": user.name if user else None,
                "user_email": user.email if user else None,
            }
        )

    return result


@router.post("/{group_id}/members", response_model=schemas.GroupMemberOut)
async def add_group_member(
    group_id: str,
    payload: schemas.GroupMemberCreate,
    current_user=Depends(get_current_user),
):
    """Add a member to a group. Only admins can add members."""
    try:
        group_uuid = UUID(group_id)
        user_uuid = UUID(payload.user_id)
    except ValueError:
        raise HTTPException(status_code=400, detail="Invalid UUID")

    # Check admin permission
    if not await GroupService.is_admin(group_uuid, current_user.id):
        raise HTTPException(status_code=403, detail="Only admins can add members")

    # Check if user exists
    user = await models.User.get(user_uuid)
    if not user:
        raise HTTPException(status_code=404, detail="User not found")

    member = await GroupService.add_member(group_uuid, user_uuid, payload.role)
    return member


@router.delete("/{group_id}/members/{user_id}")
async def remove_group_member(
    group_id: str,
    user_id: str,
    current_user=Depends(get_current_user),
):
    """Remove a member from a group. Admins can remove anyone, members can only remove themselves."""
    try:
        group_uuid = UUID(group_id)
        user_uuid = UUID(user_id)
    except ValueError:
        raise HTTPException(status_code=400, detail="Invalid UUID")

    is_admin = await GroupService.is_admin(group_uuid, current_user.id)
    is_self = current_user.id == user_uuid

    if not is_admin and not is_self:
        raise HTTPException(
            status_code=403, detail="Only admins can remove other members"
        )

    success = await GroupService.remove_member(group_uuid, user_uuid)
    if not success:
        raise HTTPException(status_code=404, detail="Member not found")

    return {"status": "removed"}


# Group Expenses
@router.get("/{group_id}/expenses", response_model=List[schemas.TransactionOut])
async def get_group_expenses(group_id: str, current_user=Depends(get_current_user)):
    """Get all expenses for a group."""
    try:
        uuid_obj = UUID(group_id)
    except ValueError:
        raise HTTPException(status_code=404, detail="Invalid UUID")

    # Check membership
    if not await GroupService.is_member(uuid_obj, current_user.id):
        raise HTTPException(status_code=403, detail="Not a member of this group")

    expenses = await GroupService.get_group_expenses(uuid_obj)
    return expenses


# Group Balances
@router.get("/{group_id}/balances", response_model=schemas.GroupBalanceSummary)
async def get_group_balances(group_id: str, current_user=Depends(get_current_user)):
    """
    Get balance summary for a group.
    Shows how much each member owes or is owed.
    """
    try:
        uuid_obj = UUID(group_id)
    except ValueError:
        raise HTTPException(status_code=404, detail="Invalid UUID")

    # Check membership
    if not await GroupService.is_member(uuid_obj, current_user.id):
        raise HTTPException(status_code=403, detail="Not a member of this group")

    g = await GroupService.get_group(uuid_obj)
    if not g:
        raise HTTPException(status_code=404, detail="Group not found")

    total = await GroupService.get_group_total_expenses(uuid_obj)
    balances_raw = await GroupService.calculate_group_balances(uuid_obj)

    # Build user balance list with names
    balances = []
    for user_id, balance in balances_raw.items():
        user = await models.User.get(user_id)
        balances.append(
            {
                "user_id": str(user_id),
                "user_name": user.name if user else "Unknown",
                "balance": round(balance, 2),
            }
        )

    return {
        "group_id": str(uuid_obj),
        "group_name": g.name,
        "total_expenses": total,
        "balances": balances,
    }


@router.post("/{group_id}/expenses")
async def add_group_expense(
    group_id: str,
    payload: schemas.TransactionCreate,
    current_user=Depends(get_current_user),
):
    """Add an expense to a group."""
    try:
        group_uuid = UUID(group_id)
    except ValueError:
        raise HTTPException(status_code=400, detail="Invalid group_id UUID")

    # Check membership
    if not await GroupService.is_member(group_uuid, current_user.id):
        raise HTTPException(status_code=403, detail="Not a member of this group")

    # Parse optional category UUID
    category_uuid = None
    wallet_uuid = None
    to_wallet_uuid = None
    if payload.category_id:
        try:
            category_uuid = UUID(payload.category_id)
        except ValueError:
            raise HTTPException(status_code=400, detail="Invalid category_id UUID")

    if payload.wallet_id:
        try:
            wallet_uuid = UUID(payload.wallet_id)
        except ValueError:
            raise HTTPException(status_code=400, detail="Invalid wallet_id UUID")

    if payload.to_wallet_id:
        try:
            to_wallet_uuid = UUID(payload.to_wallet_id)
        except ValueError:
            raise HTTPException(status_code=400, detail="Invalid to_wallet_id UUID")

    from datetime import datetime

    txn = models.Transaction(
        user_id=current_user.id,
        group_id=group_uuid,
        amount=payload.amount,
        currency=payload.currency,
        type=payload.type or "expense",
        date=payload.date or datetime.utcnow(),
        note=payload.note,
        title=payload.title,
        description=payload.description,
        split_strategy=payload.split_strategy,
        bill_image_url=payload.bill_image_url,
        wallet_id=wallet_uuid,
        to_wallet_id=to_wallet_uuid,
        category_id=category_uuid,
    )

    await txn.create()
    return {"status": "created", "id": str(txn.id)}


@router.post("/{group_id}/simplify-debts", response_model=schemas.SimplifyDebtsResponse)
async def simplify_group_debts(group_id: str, current_user=Depends(get_current_user)):
    """
    Calculate simplified debt settlements for a group.
    Uses a greedy algorithm to minimize the number of transactions
    needed to settle all balances.
    """
    try:
        uuid_obj = UUID(group_id)
    except ValueError:
        raise HTTPException(status_code=404, detail="Invalid UUID")

    # Check membership
    if not await GroupService.is_member(uuid_obj, current_user.id):
        raise HTTPException(status_code=403, detail="Not a member of this group")

    g = await GroupService.get_group(uuid_obj)
    if not g:
        raise HTTPException(status_code=404, detail="Group not found")

    # Get simplified debts
    simplified = await GroupService.simplify_debts(uuid_obj)

    # Convert to response format
    simplified_debts = [
        {
            "from_user_id": str(s["from_id"]),
            "from_user_name": s["from_name"],
            "to_user_id": str(s["to_id"]),
            "to_user_name": s["to_name"],
            "amount": s["amount"],
        }
        for s in simplified
    ]

    return {
        "group_id": str(uuid_obj),
        "group_name": g.name,
        "simplified_debts": simplified_debts,
        "total_transactions": len(simplified_debts),
    }
