"""
Wallet service - handles wallet CRUD operations.
Async/Beanie implementation.
"""

from typing import List, Optional
from uuid import UUID
from datetime import datetime

from ..models import db_models as models


class WalletService:
    """Async wallet service using Beanie ODM."""

    @staticmethod
    async def create_wallet(
        user_id: UUID,
        name: str,
        wallet_type: str = "personal",
        currency: str = "INR",
    ) -> models.Wallet:
        """Create a new wallet."""
        w = models.Wallet(
            user_id=user_id,
            name=name,
            type=wallet_type,
            currency=currency,
        )
        await w.create()
        return w

    @staticmethod
    async def get_wallets(user_id: UUID) -> List[models.Wallet]:
        """Get all non-deleted wallets for a user."""
        return await models.Wallet.find(
            models.Wallet.user_id == user_id,
            models.Wallet.deleted == False,
        ).to_list()

    @staticmethod
    async def get_wallet(wallet_id: UUID, user_id: UUID) -> Optional[models.Wallet]:
        """Get a single wallet by ID."""
        return await models.Wallet.find_one(
            models.Wallet.id == wallet_id,
            models.Wallet.user_id == user_id,
            models.Wallet.deleted == False,
        )

    @staticmethod
    async def update_wallet(
        wallet: models.Wallet,
        name: Optional[str] = None,
        wallet_type: Optional[str] = None,
        currency: Optional[str] = None,
    ) -> models.Wallet:
        """Update wallet fields."""
        if name is not None:
            wallet.name = name
        if wallet_type is not None:
            wallet.type = wallet_type
        if currency is not None:
            wallet.currency = currency
        wallet.updated_at = datetime.utcnow()
        await wallet.save()
        return wallet

    @staticmethod
    async def delete_wallet(wallet: models.Wallet) -> None:
        """Soft delete a wallet."""
        wallet.deleted = True
        wallet.updated_at = datetime.utcnow()
        await wallet.save()

    @staticmethod
    async def update_balance(
        wallet: models.Wallet, amount: float, txn_type: str
    ) -> None:
        """Update wallet balance based on transaction type."""
        if txn_type == "income":
            wallet.balance += amount
        elif txn_type == "expense":
            wallet.balance -= amount
        wallet.updated_at = datetime.utcnow()
        await wallet.save()

    @staticmethod
    async def adjust_balance(wallet: models.Wallet, delta: float) -> None:
        """Directly adjust wallet balance by delta amount."""
        wallet.balance += delta
        wallet.updated_at = datetime.utcnow()
        await wallet.save()

    @staticmethod
    async def get_total_balance(user_id: UUID, currency: str = "INR") -> float:
        """Get total balance across all wallets for a user in a specific currency."""
        wallets = await models.Wallet.find(
            models.Wallet.user_id == user_id,
            models.Wallet.currency == currency,
            models.Wallet.deleted == False,
        ).to_list()
        return sum(w.balance for w in wallets)

    @staticmethod
    async def seed_default_wallet(user_id: UUID) -> models.Wallet:
        """Create a default Cash wallet for new users."""
        # Check if user already has wallets
        existing = await models.Wallet.find(
            models.Wallet.user_id == user_id,
            models.Wallet.deleted == False,
        ).first_or_none()

        if existing:
            return existing

        # Create default Cash wallet
        wallet = models.Wallet(
            user_id=user_id,
            name="Cash",
            type="cash",
            currency="INR",
            balance=0.0,
        )
        await wallet.create()
        return wallet
