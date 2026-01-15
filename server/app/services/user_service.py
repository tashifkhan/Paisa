"""
User service - handles user profile operations.
Async/Beanie implementation.
"""

from typing import Optional
from uuid import UUID
from datetime import datetime

from ..models import db_models as models


class UserService:
    """Async user service using Beanie ODM."""

    @staticmethod
    async def get_user_by_id(user_id: str) -> Optional[models.User]:
        """Get user by ID."""
        try:
            uuid_obj = UUID(user_id)
            return await models.User.get(uuid_obj)
        except ValueError:
            return None

    @staticmethod
    async def update_user(
        user: models.User,
        name: Optional[str] = None,
        currency: Optional[str] = None,
        language: Optional[str] = None,
    ) -> models.User:
        """Update user profile fields."""
        if name is not None:
            user.name = name
        if currency is not None:
            user.currency = currency
        if language is not None:
            user.language = language
        await user.save()
        return user

    @staticmethod
    async def update_device_token(user: models.User, token: str) -> None:
        """Update user's push notification device token."""
        user.device_token = token
        await user.save()

    @staticmethod
    async def update_last_synced(user: models.User) -> None:
        """Update user's last synced timestamp."""
        user.last_synced_at = datetime.utcnow()
        await user.save()

    @staticmethod
    async def search_users(query: str, limit: int = 10) -> list[models.User]:
        """Search users by name or email."""
        # Using regex for case-insensitive partial matching
        users = (
            await models.User.find(
                {
                    "$or": [
                        {"name": {"$regex": query, "$options": "i"}},
                        {"email": {"$regex": query, "$options": "i"}},
                    ]
                }
            )
            .limit(limit)
            .to_list()
        )
        return users
