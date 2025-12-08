"""
User service - handles user profile operations.
"""

from sqlalchemy.orm import Session
from ..models import db_models as models


class UserService:
    def __init__(self, db: Session):
        self.db = db

    def get_user_by_id(self, user_id: str) -> models.User | None:
        """Get user by ID."""
        return self.db.query(models.User).filter(models.User.id == user_id).first()

    def update_user(
        self,
        user: models.User,
        name: str | None = None,
        currency: str | None = None,
        language: str | None = None,
    ) -> models.User:
        """Update user profile fields."""
        if name is not None:
            user.name = name
        if currency is not None:
            user.currency = currency
        if language is not None:
            user.language = language
        self.db.commit()
        self.db.refresh(user)
        return user

    def update_device_token(self, user: models.User, token: str) -> None:
        """Update user's push notification device token."""
        user.device_token = token
        self.db.commit()
