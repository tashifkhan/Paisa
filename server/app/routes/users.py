from fastapi import APIRouter, Depends, HTTPException, Query
from uuid import UUID

from ..core.deps import get_current_user
from ..models import db_models as models
from ..models.schemas import PushTokenUpdate, UserOut, UserUpdate
from ..services.user_service import UserService

router = APIRouter(prefix="/users", tags=["users"])


@router.put("/push-token", response_model=dict)
async def update_push_token(
    token_data: PushTokenUpdate,
    current_user=Depends(get_current_user),
):
    current_user.device_token = token_data.token
    await current_user.save()
    return {
        "status": "Token updated",
    }


@router.get("/me", response_model=UserOut)
async def read_me(current_user=Depends(get_current_user)):
    return current_user


@router.put("/me", response_model=UserOut)
async def update_me(
    payload: UserUpdate,
    current_user=Depends(get_current_user),
):
    if payload.name is not None:
        current_user.name = payload.name
    if payload.currency is not None:
        current_user.currency = payload.currency
    if payload.language is not None:
        current_user.language = payload.language
    await current_user.save()
    return current_user


@router.get("/{user_id}", response_model=UserOut)
async def get_user(user_id: str):
    try:
        uuid_obj = UUID(user_id)
    except ValueError:
        raise HTTPException(status_code=404, detail="Invalid UUID format")

    u = await models.User.get(uuid_obj)
    if not u:
        raise HTTPException(
            status_code=404,
            detail="User not found",
        )
    return u


@router.get("/search/", response_model=list[UserOut])
async def search_users(
    q: str = Query(..., min_length=1),
    current_user=Depends(get_current_user),
):
    """Search users by name or email."""
    return await UserService.search_users(q)
