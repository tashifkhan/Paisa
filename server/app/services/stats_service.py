"""
Stats service - handles statistics and summary calculations.
Async/Beanie implementation.
"""

from datetime import datetime, timedelta
from collections import defaultdict
from typing import Dict, List, Optional
from uuid import UUID

from ..models import db_models as models


class StatsService:
    """Async stats service using Beanie ODM."""

    @staticmethod
    async def get_monthly_summary(user_id: UUID, days: int = 30) -> Dict[str, float]:
        """Get transaction totals by type for the last N days."""
        since = datetime.utcnow() - timedelta(days=days)
        txns = await models.Transaction.find(
            models.Transaction.user_id == user_id,
            models.Transaction.date >= since,
            models.Transaction.deleted == False,
        ).to_list()

        totals = defaultdict(float)
        for t in txns:
            if t.type:
                totals[t.type] += t.amount or 0.0
        return dict(totals)

    @staticmethod
    async def get_category_breakdown(
        user_id: UUID,
        days: int = 30,
        txn_type: Optional[str] = "expense",
    ) -> List[dict]:
        """Get spending breakdown by category."""
        since = datetime.utcnow() - timedelta(days=days)

        query = {
            "user_id": user_id,
            "date": {"$gte": since},
            "deleted": False,
        }
        if txn_type:
            query["type"] = txn_type

        txns = await models.Transaction.find(query).to_list()

        # Group by category
        by_category: Dict[Optional[UUID], dict] = {}
        total = 0.0

        for t in txns:
            cat_id = t.category_id
            if cat_id not in by_category:
                by_category[cat_id] = {"total": 0.0, "count": 0}
            by_category[cat_id]["total"] += t.amount or 0.0
            by_category[cat_id]["count"] += 1
            total += t.amount or 0.0

        # Get category names
        result = []
        for cat_id, data in by_category.items():
            cat_name = "Uncategorized"
            if cat_id:
                category = await models.Category.get(cat_id)
                if category:
                    cat_name = category.name

            percentage = (data["total"] / total * 100) if total > 0 else 0
            result.append(
                {
                    "category_id": str(cat_id) if cat_id else None,
                    "category_name": cat_name,
                    "total": data["total"],
                    "percentage": round(percentage, 2),
                    "count": data["count"],
                }
            )

        # Sort by total descending
        result.sort(key=lambda x: x["total"], reverse=True)
        return result

    @staticmethod
    async def get_wallet_breakdown(user_id: UUID, days: int = 30) -> List[dict]:
        """Get spending breakdown by wallet."""
        since = datetime.utcnow() - timedelta(days=days)

        txns = await models.Transaction.find(
            models.Transaction.user_id == user_id,
            models.Transaction.date >= since,
            models.Transaction.deleted == False,
        ).to_list()

        # Group by wallet
        by_wallet: Dict[Optional[UUID], dict] = defaultdict(
            lambda: {"income": 0.0, "expense": 0.0, "count": 0}
        )

        for t in txns:
            wallet_id = t.wallet_id
            by_wallet[wallet_id][t.type] += t.amount or 0.0
            by_wallet[wallet_id]["count"] += 1

        # Get wallet names
        result = []
        for wallet_id, data in by_wallet.items():
            wallet_name = "No Wallet"
            if wallet_id:
                wallet = await models.Wallet.get(wallet_id)
                if wallet:
                    wallet_name = wallet.name

            result.append(
                {
                    "wallet_id": str(wallet_id) if wallet_id else None,
                    "wallet_name": wallet_name,
                    "income": data["income"],
                    "expense": data["expense"],
                    "net": data["income"] - data["expense"],
                    "count": data["count"],
                }
            )

        result.sort(key=lambda x: x["expense"], reverse=True)
        return result

    @staticmethod
    async def get_trends(user_id: UUID, months: int = 6) -> List[dict]:
        """Get monthly trends for income and expense."""
        result = []
        now = datetime.utcnow()

        for i in range(months):
            # Calculate month boundaries
            month_end = now.replace(day=1) - timedelta(days=1) if i > 0 else now
            for _ in range(i):
                month_end = month_end.replace(day=1) - timedelta(days=1)

            month_start = month_end.replace(day=1)
            month_end = (month_start + timedelta(days=32)).replace(day=1) - timedelta(
                days=1
            )

            txns = await models.Transaction.find(
                models.Transaction.user_id == user_id,
                models.Transaction.date >= month_start,
                models.Transaction.date <= month_end,
                models.Transaction.deleted == False,
            ).to_list()

            income = sum(t.amount for t in txns if t.type == "income")
            expense = sum(t.amount for t in txns if t.type == "expense")

            result.append(
                {
                    "month": month_start.strftime("%Y-%m"),
                    "month_name": month_start.strftime("%B %Y"),
                    "income": income,
                    "expense": expense,
                    "net": income - expense,
                }
            )

        # Reverse to show oldest first
        result.reverse()
        return result

    @staticmethod
    async def get_date_range_summary(
        user_id: UUID,
        start_date: datetime,
        end_date: datetime,
    ) -> dict:
        """Get summary for a specific date range."""
        txns = await models.Transaction.find(
            models.Transaction.user_id == user_id,
            models.Transaction.date >= start_date,
            models.Transaction.date <= end_date,
            models.Transaction.deleted == False,
        ).to_list()

        income = sum(t.amount for t in txns if t.type == "income")
        expense = sum(t.amount for t in txns if t.type == "expense")
        transaction_count = len(txns)

        return {
            "start_date": start_date.isoformat(),
            "end_date": end_date.isoformat(),
            "income": income,
            "expense": expense,
            "net": income - expense,
            "transaction_count": transaction_count,
        }

    @staticmethod
    async def get_full_stats(user_id: UUID, days: int = 30) -> dict:
        """Get comprehensive statistics."""
        summary = await StatsService.get_monthly_summary(user_id, days)
        categories = await StatsService.get_category_breakdown(user_id, days)

        income = summary.get("income", 0.0)
        expense = summary.get("expense", 0.0)

        return {
            "period_days": days,
            "total_expense": expense,
            "total_income": income,
            "net": income - expense,
            "by_category": categories,
        }
