from fastapi import APIRouter, Depends, Query
from datetime import datetime, timedelta
from typing import Optional

from ..core.deps import get_current_user
from ..services.stats_service import StatsService

router = APIRouter(prefix="/stats", tags=["stats"])


@router.get("/summary")
async def summary_monthly(
    current_user=Depends(get_current_user),
    days: int = Query(30, ge=1, le=365, description="Number of days to summarize"),
):
    """Get basic monthly summary with totals by transaction type."""
    totals = await StatsService.get_monthly_summary(current_user.id, days)
    return {
        "period_days": days,
        "totals": totals,
    }


@router.get("/full")
async def full_stats(
    current_user=Depends(get_current_user),
    days: int = Query(30, ge=1, le=365, description="Number of days to analyze"),
):
    """Get comprehensive statistics including category breakdown."""
    return await StatsService.get_full_stats(current_user.id, days)


@router.get("/category")
async def category_breakdown(
    current_user=Depends(get_current_user),
    days: int = Query(30, ge=1, le=365, description="Number of days to analyze"),
    type: Optional[str] = Query("expense", description="Transaction type filter"),
):
    """Get spending breakdown by category."""
    categories = await StatsService.get_category_breakdown(current_user.id, days, type)
    return {
        "period_days": days,
        "type": type,
        "categories": categories,
    }


@router.get("/wallet")
async def wallet_breakdown(
    current_user=Depends(get_current_user),
    days: int = Query(30, ge=1, le=365, description="Number of days to analyze"),
):
    """Get spending breakdown by wallet."""
    wallets = await StatsService.get_wallet_breakdown(current_user.id, days)
    return {
        "period_days": days,
        "wallets": wallets,
    }


@router.get("/trends")
async def monthly_trends(
    current_user=Depends(get_current_user),
    months: int = Query(6, ge=1, le=24, description="Number of months to show"),
):
    """Get monthly income/expense trends."""
    trends = await StatsService.get_trends(current_user.id, months)
    return {
        "months": months,
        "trends": trends,
    }


@router.get("/range")
async def date_range_summary(
    current_user=Depends(get_current_user),
    start_date: datetime = Query(..., description="Start date"),
    end_date: datetime = Query(..., description="End date"),
):
    """Get summary for a specific date range."""
    if start_date > end_date:
        start_date, end_date = end_date, start_date

    return await StatsService.get_date_range_summary(
        current_user.id, start_date, end_date
    )


@router.get("/comparison")
async def period_comparison(
    current_user=Depends(get_current_user),
    days: int = Query(30, ge=1, le=365, description="Period length in days"),
):
    """Compare current period with previous period."""
    current_totals = await StatsService.get_monthly_summary(current_user.id, days)

    # Get previous period
    now = datetime.utcnow()
    prev_end = now - timedelta(days=days)
    prev_start = prev_end - timedelta(days=days)
    prev_summary = await StatsService.get_date_range_summary(
        current_user.id, prev_start, prev_end
    )

    current_income = current_totals.get("income", 0)
    current_expense = current_totals.get("expense", 0)
    prev_income = prev_summary.get("income", 0)
    prev_expense = prev_summary.get("expense", 0)

    def calc_change(current: float, previous: float) -> dict:
        if previous == 0:
            pct = 100 if current > 0 else 0
        else:
            pct = round((current - previous) / previous * 100, 2)
        return {
            "current": current,
            "previous": previous,
            "change": round(current - previous, 2),
            "change_percent": pct,
        }

    return {
        "period_days": days,
        "income": calc_change(current_income, prev_income),
        "expense": calc_change(current_expense, prev_expense),
        "net": {
            "current": current_income - current_expense,
            "previous": prev_income - prev_expense,
        },
    }
