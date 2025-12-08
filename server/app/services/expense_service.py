"""
Expense/Transaction service - handles transaction CRUD operations.
"""

from sqlalchemy.orm import Session
from typing import List
from datetime import datetime

from ..models import db_models as models


class ExpenseService:
    def __init__(self, db: Session):
        self.db = db

    def create_transaction(
        self,
        user_id: str,
        amount: float,
        currency: str,
        type: str,
        date: datetime | None = None,
        note: str | None = None,
        split_strategy: str = "equal",
    ) -> models.Transaction:
        """Create a new transaction."""
        txn = models.Transaction(
            user_id=user_id,
            amount=amount,
            currency=currency,
            type=type,
            date=date,
            note=note,
            split_strategy=split_strategy,
        )
        self.db.add(txn)
        self.db.commit()
        self.db.refresh(txn)
        return txn

    def get_transactions(self, user_id: str) -> List[models.Transaction]:
        """Get all non-deleted transactions for a user."""
        return (
            self.db.query(models.Transaction)
            .filter(
                models.Transaction.user_id == user_id,
                models.Transaction.deleted == False,
            )
            .order_by(models.Transaction.date.desc())
            .all()
        )

    def get_transaction(self, txn_id: str, user_id: str) -> models.Transaction | None:
        """Get a single transaction by ID."""
        return (
            self.db.query(models.Transaction)
            .filter(
                models.Transaction.id == txn_id,
                models.Transaction.user_id == user_id,
                models.Transaction.deleted == False,
            )
            .first()
        )

    def update_transaction(
        self, txn: models.Transaction, **kwargs
    ) -> models.Transaction:
        """Update transaction fields."""
        for k, v in kwargs.items():
            if v is not None:
                setattr(txn, k, v)
        self.db.commit()
        self.db.refresh(txn)
        return txn

    def delete_transaction(self, txn: models.Transaction) -> None:
        """Soft delete a transaction."""
        txn.deleted = True
        self.db.commit()

    def get_all_transactions(self, user_id: str) -> List[models.Transaction]:
        """Get all transactions including deleted for export."""
        return (
            self.db.query(models.Transaction)
            .filter(models.Transaction.user_id == user_id)
            .all()
        )
