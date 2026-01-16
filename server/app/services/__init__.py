"""
Services layer - contains business logic separated from routes.
Import individual services as needed.
All services use async Beanie ODM for MongoDB operations.
"""

from .auth_service import AuthService
from .user_service import UserService
from .expense_service import ExpenseService
from .wallet_service import WalletService
from .group_service import GroupService
from .stats_service import StatsService
from .debt_service import DebtService
from .email_service import EmailService

__all__ = [
    "AuthService",
    "UserService",
    "ExpenseService",
    "WalletService",
    "GroupService",
    "StatsService",
    "DebtService",
    "EmailService",
]
