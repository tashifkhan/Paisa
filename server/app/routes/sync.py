from fastapi import APIRouter, Depends

from ..core.deps import get_current_user

router = APIRouter(prefix="/sync", tags=["sync"])


@router.post("/")
async def sync_data(payload: dict, current_user=Depends(get_current_user)):
    # Simplified sync: real implementation would handle change sets, conflict resolution
    return {"status": "synced", "changes": []}
