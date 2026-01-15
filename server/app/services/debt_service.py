"""
Debt service - handles debt/loan CRUD operations.
Async/Beanie implementation.
"""

from typing import List, Optional, Literal
from datetime import datetime
from uuid import UUID

from ..models import db_models as models


class DebtService:
    """Async debt service using Beanie ODM."""

    @staticmethod
    async def create_debt(
        user_id: UUID,
        counterparty_name: str,
        amount: float,
        debt_type: Literal["owed_to_me", "owed_by_me"],
        due_date: Optional[str] = None,
    ) -> models.Debt:
        """Create a new debt record."""
        d = models.Debt(
            user_id=user_id,
            counterparty_name=counterparty_name,
            amount=amount,
            type=debt_type,
            due_date=due_date,
        )
        await d.create()
        return d

    @staticmethod
    async def get_debts(user_id: UUID) -> List[models.Debt]:
        """Get all non-deleted debts for a user."""
        return await models.Debt.find(
            models.Debt.user_id == user_id,
            models.Debt.deleted == False,
        ).to_list()

    @staticmethod
    async def get_debt(debt_id: UUID, user_id: UUID) -> Optional[models.Debt]:
        """Get a single debt by ID."""
        return await models.Debt.find_one(
            models.Debt.id == debt_id,
            models.Debt.user_id == user_id,
            models.Debt.deleted == False,
        )

    @staticmethod
    async def update_debt(
        debt: models.Debt,
        counterparty_name: Optional[str] = None,
        amount: Optional[float] = None,
        debt_type: Optional[Literal["owed_to_me", "owed_by_me"]] = None,
        due_date: Optional[str] = None,
    ) -> models.Debt:
        """Update debt fields."""
        if counterparty_name is not None:
            debt.counterparty_name = counterparty_name
        if amount is not None:
            debt.amount = amount
        if debt_type is not None:
            debt.type = debt_type
        if due_date is not None:
            debt.due_date = due_date
        debt.updated_at = datetime.utcnow()
        await debt.save()
        return debt

    @staticmethod
    async def delete_debt(debt: models.Debt) -> None:
        """Soft delete a debt."""
        debt.deleted = True
        debt.updated_at = datetime.utcnow()
        await debt.save()

    @staticmethod
    async def get_debts_owed_to_me(user_id: UUID) -> List[models.Debt]:
        """Get all debts where money is owed to the user."""
        return await models.Debt.find(
            models.Debt.user_id == user_id,
            models.Debt.type == "owed_to_me",
            models.Debt.deleted == False,
        ).to_list()

    @staticmethod
    async def get_debts_owed_by_me(user_id: UUID) -> List[models.Debt]:
        """Get all debts where user owes money."""
        return await models.Debt.find(
            models.Debt.user_id == user_id,
            models.Debt.type == "owed_by_me",
            models.Debt.deleted == False,
        ).to_list()

    @staticmethod
    async def get_total_owed_to_me(user_id: UUID) -> float:
        """Get total amount owed to the user."""
        debts = await DebtService.get_debts_owed_to_me(user_id)
        return sum(d.amount for d in debts)

    @staticmethod
    async def get_total_owed_by_me(user_id: UUID) -> float:
        """Get total amount the user owes."""
        debts = await DebtService.get_debts_owed_by_me(user_id)
        return sum(d.amount for d in debts)
