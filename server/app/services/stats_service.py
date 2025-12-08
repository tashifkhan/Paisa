"""
Stats service - handles statistics and summary calculations.
"""

from sqlalchemy.orm import Session
from datetime import datetime, timedelta
from collections import defaultdict
from typing import Dict

from ..models import db_models as models


class StatsService:
    def __init__(self, db: Session):
        self.db = db

    def get_monthly_summary(self, user_id: str, days: int = 30) -> Dict[str, float]:
        """Get transaction totals by type for the last N days."""
        since = datetime.utcnow() - timedelta(days=days)
        txns = (
            self.db.query(models.Transaction)
            .filter(
                models.Transaction.user_id == user_id,
                models.Transaction.date >= since,
                models.Transaction.deleted == False,
            )
            .all()
        )
        totals = defaultdict(float)
        for t in txns:
            if t.type:
                totals[t.type] += t.amount or 0.0
        return dict(totals)
