from fastapi import APIRouter, Depends, HTTPException
from typing import List
from uuid import UUID

from ..core.deps import get_current_user
from ..models import db_models as models
from ..models import schemas

router = APIRouter(prefix="/groups", tags=["groups"])


@router.post("/", response_model=dict)
async def create_group(
    payload: schemas.GroupCreate,
    current_user=Depends(get_current_user),
):
    g = models.Group(
        name=payload.name,
        base_currency=payload.base_currency,
        created_by_user_id=current_user.id,
    )
    await g.create()
    return {
        "status": "created",
        "id": str(g.id),
    }


@router.get("/", response_model=List[schemas.GroupOut])
async def list_groups(current_user=Depends(get_current_user)):
    # Assuming explicit per-user groups or global? Original code was global `filter(deleted=False)`
    # but that seems like a security issue if everyone sees all groups.
    # Keeping original logic (all non-deleted groups visible to any auth user) for parity.
    groups = await models.Group.find(models.Group.deleted == False).to_list()
    return groups


@router.get("/{group_id}", response_model=schemas.GroupOut)
async def get_group(group_id: str, current_user=Depends(get_current_user)):
    try:
        uuid_obj = UUID(group_id)
    except ValueError:
        raise HTTPException(status_code=404, detail="Invalid UUID")

    g = await models.Group.find_one(
        models.Group.id == uuid_obj, models.Group.deleted == False
    )

    if not g:
        raise HTTPException(
            status_code=404,
            detail="Group not found",
        )
    return g


@router.delete("/{group_id}", response_model=dict)
async def delete_group(group_id: str, current_user=Depends(get_current_user)):
    try:
        uuid_obj = UUID(group_id)
    except ValueError:
        raise HTTPException(status_code=404, detail="Invalid UUID")

    g = await models.Group.find_one(models.Group.id == uuid_obj)
    if not g:
        raise HTTPException(
            status_code=404,
            detail="Group not found",
        )

    # Ideally check permission (created_by_user_id == current_user.id)
    # But sticking to original logic parity which didn't verify owner explicitly in the delete route
    # (wait, original used `db.query(models.Group).filter(models.Group.id == group_id).first()`, no user check).

    g.deleted = True
    await g.save()
    return {
        "status": "deleted",
    }
