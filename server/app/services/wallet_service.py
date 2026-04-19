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
    def _is_credit_wallet(wallet_type: Optional[str]) -> bool:
        return (wallet_type or "").lower() in {"credit", "credit_card", "credit-card"}

    @staticmethod
    def _normalize_wallet_type(wallet_type: Optional[str]) -> str:
        if WalletService._is_credit_wallet(wallet_type):
            return "credit_card"
        return wallet_type or "personal"

    @staticmethod
    def _normalize_credit_fields(
        wallet_type: Optional[str],
        credit_limit: Optional[float],
        statement_day: Optional[int],
        due_day: Optional[int],
    ) -> tuple[Optional[float], Optional[int], Optional[int]]:
        is_credit = WalletService._is_credit_wallet(wallet_type)
        if not is_credit:
            return None, None, None

        if credit_limit is not None and credit_limit < 0:
            raise ValueError("credit_limit must be >= 0")
        if statement_day is not None and (statement_day < 1 or statement_day > 31):
            raise ValueError("statement_day must be between 1 and 31")
        if due_day is not None and (due_day < 1 or due_day > 31):
            raise ValueError("due_day must be between 1 and 31")

        return credit_limit, statement_day, due_day

    @staticmethod
    def compute_available_credit(wallet: models.Wallet) -> Optional[float]:
        if not WalletService._is_credit_wallet(wallet.type):
            return None
        if wallet.credit_limit is None:
            return None
        used = max(0.0, -wallet.balance)
        return max(0.0, wallet.credit_limit - used)

    @staticmethod
    def compute_utilization_percent(wallet: models.Wallet) -> Optional[float]:
        if not WalletService._is_credit_wallet(wallet.type):
            return None
        if wallet.credit_limit is None or wallet.credit_limit <= 0:
            return None
        used = max(0.0, -wallet.balance)
        return round((used / wallet.credit_limit) * 100, 2)

    @staticmethod
    async def create_wallet(
        user_id: UUID,
        name: str,
        wallet_type: str = "personal",
        currency: str = "INR",
        credit_limit: Optional[float] = None,
        statement_day: Optional[int] = None,
        due_day: Optional[int] = None,
    ) -> models.Wallet:
        """Create a new wallet."""
        wallet_type = WalletService._normalize_wallet_type(wallet_type)
        credit_limit, statement_day, due_day = WalletService._normalize_credit_fields(
            wallet_type, credit_limit, statement_day, due_day
        )

        w = models.Wallet(
            user_id=user_id,
            name=name,
            type=wallet_type,
            currency=currency,
            credit_limit=credit_limit,
            statement_day=statement_day,
            due_day=due_day,
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
        credit_limit: Optional[float] = None,
        statement_day: Optional[int] = None,
        due_day: Optional[int] = None,
    ) -> models.Wallet:
        """Update wallet fields."""
        if name is not None:
            wallet.name = name
        if wallet_type is not None:
            wallet.type = WalletService._normalize_wallet_type(wallet_type)
        if currency is not None:
            wallet.currency = currency

        final_type = wallet.type
        final_credit_limit = (
            wallet.credit_limit if credit_limit is None else credit_limit
        )
        final_statement_day = (
            wallet.statement_day if statement_day is None else statement_day
        )
        final_due_day = wallet.due_day if due_day is None else due_day

        normalized_limit, normalized_statement_day, normalized_due_day = (
            WalletService._normalize_credit_fields(
                final_type,
                final_credit_limit,
                final_statement_day,
                final_due_day,
            )
        )
        wallet.credit_limit = normalized_limit
        wallet.statement_day = normalized_statement_day
        wallet.due_day = normalized_due_day

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
        is_credit = WalletService._is_credit_wallet(wallet.type)

        if txn_type == "income":
            wallet.balance = (
                wallet.balance - amount if is_credit else wallet.balance + amount
            )
        elif txn_type == "expense":
            wallet.balance = (
                wallet.balance + amount if is_credit else wallet.balance - amount
            )

        if is_credit and wallet.credit_limit is not None:
            used = max(0.0, -wallet.balance)
            if used > wallet.credit_limit:
                raise ValueError("Credit limit exceeded")

        wallet.updated_at = datetime.utcnow()
        await wallet.save()

    @staticmethod
    async def adjust_balance(wallet: models.Wallet, delta: float) -> None:
        """Directly adjust wallet balance by delta amount."""
        new_balance = wallet.balance + delta
        is_credit = WalletService._is_credit_wallet(wallet.type)
        if is_credit and wallet.credit_limit is not None:
            used = max(0.0, -new_balance)
            if used > wallet.credit_limit:
                raise ValueError("Credit limit exceeded")

        wallet.balance = new_balance
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

        total = 0.0
        for wallet in wallets:
            if WalletService._is_credit_wallet(wallet.type):
                continue
            total += wallet.balance
        return total

    @staticmethod
    async def get_total_credit_used(user_id: UUID, currency: str = "INR") -> float:
        """Get total used credit across credit wallets in a currency."""
        wallets = await models.Wallet.find(
            models.Wallet.user_id == user_id,
            models.Wallet.currency == currency,
            models.Wallet.deleted == False,
        ).to_list()

        used = 0.0
        for wallet in wallets:
            if WalletService._is_credit_wallet(wallet.type):
                used += max(0.0, -wallet.balance)
        return used

    @staticmethod
    async def get_total_available_credit(user_id: UUID, currency: str = "INR") -> float:
        """Get total available credit across credit wallets in a currency."""
        wallets = await models.Wallet.find(
            models.Wallet.user_id == user_id,
            models.Wallet.currency == currency,
            models.Wallet.deleted == False,
        ).to_list()

        available = 0.0
        for wallet in wallets:
            avail = WalletService.compute_available_credit(wallet)
            if avail is not None:
                available += avail
        return available

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
