"""
Wallet service - handles wallet CRUD operations.
"""

from sqlalchemy.orm import Session
from typing import List

from ..models import db_models as models


class WalletService:
    def __init__(self, db: Session):
        self.db = db

    def create_wallet(
        self,
        user_id: str,
        name: str,
        type: str = "personal",
        currency: str = "INR",
    ) -> models.Wallet:
        """Create a new wallet."""
        w = models.Wallet(
            user_id=user_id,
            name=name,
            type=type,
            currency=currency,
        )
        self.db.add(w)
        self.db.commit()
        self.db.refresh(w)
        return w

    def get_wallets(self, user_id: str) -> List[models.Wallet]:
        """Get all non-deleted wallets for a user."""
        return (
            self.db.query(models.Wallet)
            .filter(
                models.Wallet.user_id == user_id,
                models.Wallet.deleted == False,
            )
            .all()
        )

    def get_wallet(self, wallet_id: str, user_id: str) -> models.Wallet | None:
        """Get a single wallet by ID."""
        return (
            self.db.query(models.Wallet)
            .filter(
                models.Wallet.id == wallet_id,
                models.Wallet.user_id == user_id,
            )
            .first()
        )

    def update_wallet(
        self,
        wallet: models.Wallet,
        name: str,
        type: str,
        currency: str,
    ) -> models.Wallet:
        """Update wallet fields."""
        wallet.name = name
        wallet.type = type
        wallet.currency = currency
        self.db.commit()
        self.db.refresh(wallet)
        return wallet

    def delete_wallet(self, wallet: models.Wallet) -> None:
        """Soft delete a wallet."""
        wallet.deleted = True
        self.db.commit()
