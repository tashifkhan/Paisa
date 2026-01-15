"""
Expense/Transaction service - handles transaction CRUD operations.
Async/Beanie implementation.
"""

from typing import List, Optional
from datetime import datetime
from uuid import UUID

from ..models import db_models as models


class ExpenseService:
    """Async expense/transaction service using Beanie ODM."""

    @staticmethod
    async def create_transaction(
        user_id: UUID,
        amount: float,
        currency: str,
        type: str,
        date: Optional[datetime] = None,
        note: Optional[str] = None,
        split_strategy: str = "equal",
        wallet_id: Optional[UUID] = None,
        group_id: Optional[UUID] = None,
    ) -> models.Transaction:
        """Create a new transaction."""
        txn = models.Transaction(
            user_id=user_id,
            amount=amount,
            currency=currency,
            type=type,
            date=date or datetime.utcnow(),
            note=note,
            split_strategy=split_strategy,
            wallet_id=wallet_id,
            group_id=group_id,
        )
        await txn.create()
        
        # Update wallet balance if wallet_id is provided
        if wallet_id:
            await ExpenseService.update_wallet_balance(wallet_id, amount, type)
        
        return txn

    @staticmethod
    async def update_wallet_balance(
        wallet_id: UUID, amount: float, txn_type: str
    ) -> None:
        """Update wallet balance based on transaction type."""
        wallet = await models.Wallet.find_one(models.Wallet.id == wallet_id)
        if wallet:
            if txn_type == "income":
                wallet.balance += amount
            elif txn_type == "expense":
                wallet.balance -= amount
            await wallet.save()

    @staticmethod
    async def get_transactions(user_id: UUID) -> List[models.Transaction]:
        """Get all non-deleted transactions for a user."""
        return (
            await models.Transaction.find(
                models.Transaction.user_id == user_id,
                models.Transaction.deleted == False,
            )
            .sort(-models.Transaction.date)
            .to_list()
        )

    @staticmethod
    async def get_transaction(
        txn_id: UUID, user_id: UUID
    ) -> Optional[models.Transaction]:
        """Get a single transaction by ID."""
        return await models.Transaction.find_one(
            models.Transaction.id == txn_id,
            models.Transaction.user_id == user_id,
            models.Transaction.deleted == False,
        )

    @staticmethod
    async def update_transaction(
        txn: models.Transaction, **kwargs
    ) -> models.Transaction:
        """Update transaction fields."""
        for k, v in kwargs.items():
            if v is not None:
                setattr(txn, k, v)
        txn.updated_at = datetime.utcnow()
        await txn.save()
        return txn

    @staticmethod
    async def delete_transaction(txn: models.Transaction) -> None:
        """Soft delete a transaction."""
        txn.deleted = True
        txn.updated_at = datetime.utcnow()
        await txn.save()

    @staticmethod
    async def get_all_transactions(user_id: UUID) -> List[models.Transaction]:
        """Get all transactions including deleted for export."""
        return await models.Transaction.find(
            models.Transaction.user_id == user_id
        ).to_list()

    @staticmethod
    async def get_transactions_by_group(group_id: UUID) -> List[models.Transaction]:
        """Get all transactions for a group."""
        return (
            await models.Transaction.find(
                models.Transaction.group_id == group_id,
                models.Transaction.deleted == False,
            )
            .sort(-models.Transaction.date)
            .to_list()
        )

    @staticmethod
    async def get_transactions_by_wallet(wallet_id: UUID) -> List[models.Transaction]:
        """Get all transactions for a wallet."""
        return (
            await models.Transaction.find(
                models.Transaction.wallet_id == wallet_id,
                models.Transaction.deleted == False,
            )
            .sort(-models.Transaction.date)
            .to_list()
        )
