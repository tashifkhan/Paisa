from fastapi import APIRouter, Depends, HTTPException
from typing import List
from uuid import UUID

from ..core.deps import get_current_user
from ..models import db_models as models
from ..models import schemas
from ..services.debt_service import DebtService

router = APIRouter(prefix="/debts", tags=["debts"])


@router.get("/", response_model=List[schemas.DebtOut])
async def list_debts(current_user=Depends(get_current_user)):
    """List all debts for the current user."""
    debts = await DebtService.get_debts(current_user.id)
    return debts


@router.get("/summary")
async def debt_summary(current_user=Depends(get_current_user)):
    """Get summary of debts owed to and by the user."""
    owed_to_me = await DebtService.get_total_owed_to_me(current_user.id)
    owed_by_me = await DebtService.get_total_owed_by_me(current_user.id)

    return {
        "owed_to_me": owed_to_me,
        "owed_by_me": owed_by_me,
        "net": owed_to_me - owed_by_me,
    }


@router.get("/owed-to-me", response_model=List[schemas.DebtOut])
async def list_debts_owed_to_me(current_user=Depends(get_current_user)):
    """List debts owed to the current user."""
    return await DebtService.get_debts_owed_to_me(current_user.id)


@router.get("/owed-by-me", response_model=List[schemas.DebtOut])
async def list_debts_owed_by_me(current_user=Depends(get_current_user)):
    """List debts the current user owes."""
    return await DebtService.get_debts_owed_by_me(current_user.id)


@router.post("/", response_model=schemas.DebtOut)
async def create_debt(
    payload: schemas.DebtCreate,
    current_user=Depends(get_current_user),
):
    """Create a new debt record."""
    d = await DebtService.create_debt(
        user_id=current_user.id,
        counterparty_name=payload.counterparty_name,
        amount=payload.amount,
        debt_type=payload.type,
        due_date=payload.due_date,
    )
    return d


@router.get("/{debt_id}", response_model=schemas.DebtOut)
async def get_debt(debt_id: str, current_user=Depends(get_current_user)):
    """Get a single debt by ID."""
    try:
        uuid_obj = UUID(debt_id)
    except ValueError:
        raise HTTPException(status_code=404, detail="Invalid UUID")

    d = await DebtService.get_debt(uuid_obj, current_user.id)
    if not d:
        raise HTTPException(status_code=404, detail="Debt not found")

    return d


@router.put("/{debt_id}", response_model=schemas.DebtOut)
async def update_debt(
    debt_id: str,
    payload: schemas.DebtUpdate,
    current_user=Depends(get_current_user),
):
    """Update a debt record."""
    try:
        uuid_obj = UUID(debt_id)
    except ValueError:
        raise HTTPException(status_code=404, detail="Invalid UUID")

    d = await DebtService.get_debt(uuid_obj, current_user.id)
    if not d:
        raise HTTPException(status_code=404, detail="Debt not found")

    d = await DebtService.update_debt(
        d,
        counterparty_name=payload.counterparty_name,
        amount=payload.amount,
        debt_type=payload.type,
        due_date=payload.due_date,
    )
    return d


@router.delete("/{debt_id}")
async def delete_debt(debt_id: str, current_user=Depends(get_current_user)):
    """Delete a debt record (soft delete)."""
    try:
        uuid_obj = UUID(debt_id)
    except ValueError:
        raise HTTPException(status_code=404, detail="Invalid UUID")

    d = await DebtService.get_debt(uuid_obj, current_user.id)
    if not d:
        raise HTTPException(status_code=404, detail="Debt not found")

    await DebtService.delete_debt(d)
    return {"status": "deleted"}


@router.post("/{debt_id}/settle")
async def settle_debt(debt_id: str, current_user=Depends(get_current_user)):
    """Mark a debt as settled (deletes it)."""
    try:
        uuid_obj = UUID(debt_id)
    except ValueError:
        raise HTTPException(status_code=404, detail="Invalid UUID")

    d = await DebtService.get_debt(uuid_obj, current_user.id)
    if not d:
        raise HTTPException(status_code=404, detail="Debt not found")

    await DebtService.delete_debt(d)
    return {
        "status": "settled",
        "amount": d.amount,
        "counterparty": d.counterparty_name,
    }
