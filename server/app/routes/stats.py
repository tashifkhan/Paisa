from fastapi import APIRouter, Depends
from datetime import datetime, timedelta
from collections import defaultdict

from ..core.deps import get_current_user
from ..models import db_models as models

router = APIRouter(prefix="/stats", tags=["stats"])


@router.get("/summary")
async def summary_monthly(current_user=Depends(get_current_user)):
    # simple last 30 days summary
    since = datetime.utcnow() - timedelta(days=30)

    # In Beanie: find query with date comparison
    # models.Transaction.date >= since
    txns = await models.Transaction.find(
        models.Transaction.user_id == current_user.id,
        models.Transaction.date >= since,
        models.Transaction.deleted == False,
    ).to_list()

    totals = defaultdict(float)

    for t in txns:
        if t.type:
            totals[t.type] += t.amount or 0.0

    return {
        "period_days": 30,
        "totals": dict(totals),
    }
