"""Paisa MCP Server — personal finance tools for AI assistants."""

from __future__ import annotations

import json
from datetime import datetime, timedelta
from typing import Annotated, Literal, Optional

from fastmcp import FastMCP

from . import client

mcp = FastMCP(
    name="Paisa",
    instructions=(
        "You have access to a personal finance app called Paisa. "
        "Use these tools to query and manage the user's expenses, wallets, debts, and groups. "
        "Currency amounts are in the user's default currency (usually INR). "
        "Dates are ISO 8601 strings. Transaction types are 'expense' or 'income'."
    ),
)


@mcp.tool()
async def get_transactions(
    limit: Annotated[int, "Max number of transactions to return (1–100)"] = 20,
    type: Annotated[
        Optional[Literal["expense", "income"]], "Filter by transaction type"
    ] = None,
    days: Annotated[
        Optional[int], "Only return transactions from the last N days"
    ] = None,
    wallet_id: Annotated[Optional[str], "Filter by wallet ID"] = None,
    category_id: Annotated[Optional[str], "Filter by category ID"] = None,
) -> list[dict]:
    """Get recent transactions. Returns amount, type, date, note, wallet_id, category_id."""
    params: dict = {"limit": min(max(limit, 1), 100)}
    if type:
        params["type"] = type
    if wallet_id:
        params["wallet_id"] = wallet_id
    if category_id:
        params["category_id"] = category_id
    if days:
        start = (datetime.utcnow() - timedelta(days=days)).date().isoformat()
        params["start_date"] = start
    return await client.get("/expenses/", params=params)


@mcp.tool()
async def add_transaction(
    amount: Annotated[float, "Amount (positive number)"],
    type: Annotated[Literal["expense", "income"], "Transaction type"],
    note: Annotated[Optional[str], "Optional description or memo"] = None,
    wallet_id: Annotated[
        Optional[str], "ID of the wallet to use (use get_wallets to find IDs)"
    ] = None,
    category_id: Annotated[
        Optional[str], "ID of the category (use get_categories to find IDs)"
    ] = None,
    date: Annotated[Optional[str], "ISO date string (defaults to today)"] = None,
    currency: Annotated[str, "Currency code (e.g. INR, USD)"] = "INR",
) -> dict:
    """Add a new expense or income transaction."""
    payload: dict = {
        "amount": amount,
        "type": type,
        "currency": currency,
        "date": date or datetime.utcnow().isoformat(),
    }
    if note:
        payload["note"] = note
    if wallet_id:
        payload["wallet_id"] = wallet_id
    if category_id:
        payload["category_id"] = category_id
    return await client.post("/expenses/add", json=payload)


@mcp.tool()
async def update_transaction(
    transaction_id: Annotated[str, "Transaction ID to update"],
    amount: Annotated[Optional[float], "New amount"] = None,
    note: Annotated[Optional[str], "New note"] = None,
    category_id: Annotated[Optional[str], "New category ID"] = None,
    date: Annotated[Optional[str], "New date (ISO string)"] = None,
) -> dict:
    """Update an existing transaction's fields."""
    payload = {
        k: v
        for k, v in {
            "amount": amount,
            "note": note,
            "category_id": category_id,
            "date": date,
        }.items()
        if v is not None
    }
    return await client.put(f"/expenses/{transaction_id}", json=payload)


@mcp.tool()
async def delete_transaction(
    transaction_id: Annotated[str, "Transaction ID to delete"],
) -> dict:
    """Permanently delete a transaction."""
    return await client.delete(f"/expenses/{transaction_id}")


@mcp.tool()
async def get_wallets() -> list[dict]:
    """Get all wallets with their balances, types, and currencies."""
    return await client.get("/wallets/")


@mcp.tool()
async def get_total_balance(
    currency: Annotated[str, "Currency to convert all balances to"] = "INR",
) -> dict:
    """Get the total balance across all wallets, converted to a single currency."""
    return await client.get(f"/wallets/total?currency={currency}")


@mcp.tool()
async def add_wallet(
    name: Annotated[str, "Wallet name (e.g. 'HDFC Savings', 'Cash')"],
    type: Annotated[Literal["card", "cash", "virtual"], "Wallet type"] = "card",
    currency: Annotated[str, "Currency code"] = "INR",
) -> dict:
    """Create a new wallet."""
    return await client.post(
        "/wallets/", json={"name": name, "type": type, "currency": currency}
    )


@mcp.tool()
async def adjust_wallet_balance(
    wallet_id: Annotated[str, "Wallet ID"],
    amount: Annotated[float, "New balance amount to set the wallet to"],
) -> dict:
    """Adjust a wallet's balance (set it to a specific value)."""
    return await client.post(f"/wallets/{wallet_id}/adjust-balance?amount={amount}")


@mcp.tool()
async def get_categories(
    type: Annotated[
        Optional[Literal["expense", "income", "both"]], "Filter by category type"
    ] = None,
) -> list[dict]:
    """Get all expense/income categories with their IDs, names, and types."""
    cats: list[dict] = await client.get("/categories/")
    if type:
        cats = [c for c in cats if c.get("type") in (type, "both")]
    return cats


@mcp.tool()
async def get_spending_stats(
    days: Annotated[int, "Number of days to analyse (e.g. 30, 60, 90)"] = 30,
) -> dict:
    """
    Get comprehensive spending statistics: total income, total expense, net,
    and a breakdown by category with percentages.
    """
    return await client.get(f"/stats/full?days={days}")


@mcp.tool()
async def get_spending_by_category(
    days: Annotated[int, "Number of days to analyse"] = 30,
    type: Annotated[
        Literal["expense", "income"], "Which type to break down"
    ] = "expense",
) -> list[dict]:
    """Get spending (or income) broken down by category with amounts and percentages."""
    data = await client.get(f"/stats/category?days={days}&type={type}")
    return data.get("categories", [])


@mcp.tool()
async def get_monthly_trends(
    months: Annotated[int, "Number of months of history to return (1–12)"] = 6,
) -> list[dict]:
    """Get month-by-month income/expense/net trend data."""
    data = await client.get(f"/stats/trends?months={months}")
    return data.get("trends", [])


@mcp.tool()
async def compare_periods(
    days: Annotated[int, "Compare current N days vs previous N days"] = 30,
) -> dict:
    """
    Compare current period vs the previous same-length period.
    Returns income/expense changes with absolute and percentage differences.
    """
    return await client.get(f"/stats/comparison?days={days}")


@mcp.tool()
async def get_spending_summary(
    days: Annotated[int, "Period to summarise in days"] = 30,
) -> str:
    """
    Generate a concise human-readable summary of spending for the given period.
    Includes totals, top categories, and comparison to previous period.
    """
    stats, comparison, categories = await _gather_summary_data(days)

    lines: list[str] = [
        f"## Spending Summary — last {days} days",
        "",
        f"**Income:**  {_fmt(stats.get('total_income', 0))}",
        f"**Expense:** {_fmt(stats.get('total_expense', 0))}",
        f"**Net:**     {_fmt_signed(stats.get('net', 0))}",
        "",
        "### vs previous period",
    ]

    inc = comparison.get("income", {})
    exp = comparison.get("expense", {})
    if inc:
        direction = "↑" if inc.get("change", 0) >= 0 else "↓"
        lines.append(
            f"Income  {direction} {abs(inc.get('change_percent', 0)):.1f}%  "
            f"({_fmt(inc.get('previous', 0))} → {_fmt(inc.get('current', 0))})"
        )
    if exp:
        direction = "↑" if exp.get("change", 0) >= 0 else "↓"
        lines.append(
            f"Expense {direction} {abs(exp.get('change_percent', 0)):.1f}%  "
            f"({_fmt(exp.get('previous', 0))} → {_fmt(exp.get('current', 0))})"
        )

    if categories:
        lines += ["", "### Top spending categories"]
        for cat in categories[:5]:
            lines.append(
                f"- {cat['category_name']}: {_fmt(cat['total'])} ({cat['percentage']:.1f}%)"
            )

    return "\n".join(lines)


async def _gather_summary_data(days: int) -> tuple[dict, dict, list[dict]]:
    import asyncio

    stats_task = client.get(f"/stats/full?days={days}")
    comp_task = client.get(f"/stats/comparison?days={days}")
    cat_task = client.get(f"/stats/category?days={days}&type=expense")

    stats, comp, cat_data = await asyncio.gather(stats_task, comp_task, cat_task)
    return stats, comp, cat_data.get("categories", [])


def _fmt(amount: float) -> str:
    return f"₹{abs(amount):,.0f}"


def _fmt_signed(amount: float) -> str:
    sign = "-" if amount < 0 else ""
    return f"{sign}₹{abs(amount):,.0f}"


# ─── Debts ───────────────────────────────────────────────────────────────────


@mcp.tool()
async def get_debts() -> list[dict]:
    """Get all personal debts — both owed to you and owed by you."""
    return await client.get("/debts/")


@mcp.tool()
async def get_debt_summary() -> dict:
    """
    Get a summary of all debts: total owed to you, total you owe, and net balance.
    """
    return await client.get("/debts/summary")


@mcp.tool()
async def add_debt(
    counterparty_name: Annotated[str, "Name of the person this debt is with"],
    amount: Annotated[float, "Amount of the debt"],
    type: Annotated[Literal["owed_to_me", "owed_by_me"], "Who owes whom"],
    due_date: Annotated[
        Optional[str], "Optional due date (ISO date string, e.g. '2026-05-01')"
    ] = None,
) -> dict:
    """Record a new personal debt."""
    payload: dict = {
        "counterparty_name": counterparty_name,
        "amount": amount,
        "type": type,
    }
    if due_date:
        payload["due_date"] = due_date
    return await client.post("/debts/", json=payload)


@mcp.tool()
async def settle_debt(
    debt_id: Annotated[
        str, "ID of the debt to mark as settled (use get_debts to find IDs)"
    ],
) -> dict:
    """Mark a debt as settled/paid."""
    return await client.post(f"/debts/{debt_id}/settle")


@mcp.tool()
async def delete_debt(
    debt_id: Annotated[str, "ID of the debt to delete"],
) -> dict:
    """Delete a debt record."""
    return await client.delete(f"/debts/{debt_id}")


# ─── Groups ──────────────────────────────────────────────────────────────────


@mcp.tool()
async def get_groups() -> list[dict]:
    """Get all shared expense groups you are a member of."""
    return await client.get("/groups/")


@mcp.tool()
async def get_group_balances(
    group_id: Annotated[str, "Group ID (use get_groups to find IDs)"],
) -> dict:
    """
    Get the balance summary for a group: total expenses and what each member owes/is owed.
    """
    return await client.get(f"/groups/{group_id}/balances")


@mcp.tool()
async def get_group_expenses(
    group_id: Annotated[str, "Group ID"],
) -> list[dict]:
    """Get all expenses in a shared group."""
    return await client.get(f"/groups/{group_id}/expenses")


@mcp.tool()
async def add_group_expense(
    group_id: Annotated[str, "Group ID"],
    amount: Annotated[float, "Expense amount"],
    note: Annotated[Optional[str], "What was this expense for?"] = None,
    split_strategy: Annotated[
        Literal["equal", "custom"], "How to split among members"
    ] = "equal",
    currency: Annotated[str, "Currency code"] = "INR",
) -> dict:
    """Add a shared expense to a group, split equally by default."""
    payload: dict = {
        "amount": amount,
        "currency": currency,
        "type": "expense",
        "date": datetime.utcnow().isoformat(),
        "split_strategy": split_strategy,
    }
    if note:
        payload["note"] = note
    return await client.post(f"/groups/{group_id}/expenses", json=payload)


@mcp.tool()
async def simplify_group_debts(
    group_id: Annotated[str, "Group ID"],
) -> dict:
    """
    Calculate the minimal set of transactions needed to settle all balances in a group.
    Returns a list of who should pay whom and how much.
    """
    return await client.post(f"/groups/{group_id}/simplify-debts")


@mcp.tool()
async def create_group(
    name: Annotated[str, "Group name (e.g. 'Goa Trip', 'Flat 302')"],
    base_currency: Annotated[str, "Currency for the group"] = "INR",
) -> dict:
    """Create a new shared expense group."""
    return await client.post(
        "/groups/", json={"name": name, "base_currency": base_currency}
    )


# ─── Users ───────────────────────────────────────────────────────────────────


@mcp.tool()
async def get_my_profile() -> dict:
    """Get your profile: name, email, default currency, language."""
    return await client.get("/users/me")


@mcp.tool()
async def search_users(
    query: Annotated[str, "Search query (name or email)"],
) -> list[dict]:
    """Search for other Paisa users by name or email (useful for adding to groups)."""
    return await client.get("/users/search/", params={"q": query})


# ─── Resources ───────────────────────────────────────────────────────────────


@mcp.resource("paisa://dashboard")
async def dashboard_resource() -> str:
    """Live financial dashboard snapshot."""
    import asyncio

    total_task = client.get("/wallets/total?currency=INR")
    stats_task = client.get("/stats/full?days=30")
    debts_task = client.get("/debts/summary")

    total, stats, debts = await asyncio.gather(total_task, stats_task, debts_task)

    data = {
        "total_balance": total,
        "last_30_days": {
            "income": stats.get("total_income"),
            "expense": stats.get("total_expense"),
            "net": stats.get("net"),
        },
        "debts": debts,
    }
    return json.dumps(data, indent=2)


@mcp.resource("paisa://wallets")
async def wallets_resource() -> str:
    """All wallets with current balances."""
    wallets = await client.get("/wallets/")
    return json.dumps(wallets, indent=2)


@mcp.resource("paisa://recent-transactions")
async def recent_transactions_resource() -> str:
    """The 20 most recent transactions."""
    txns = await client.get("/expenses/", params={"limit": 20})
    return json.dumps(txns, indent=2)


# ─── Entry point ─────────────────────────────────────────────────────────────


def main() -> None:
    import asyncio

    async def _preflight() -> None:
        try:
            email = await client.validate_auth()
            print(f"[paisa-mcp] Authenticated as {email}", flush=True)
        except Exception as exc:
            print(f"[paisa-mcp] Auth error: {exc}", flush=True)
            raise SystemExit(1) from exc

    asyncio.run(_preflight())
    mcp.run()


if __name__ == "__main__":
    main()
