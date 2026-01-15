"""
Authentication service - handles OTP, login, and token creation logic.
Async/Beanie implementation.
"""

from datetime import datetime, timedelta
from typing import Optional
import random

from ..models import db_models as models
from ..core.security import get_password_hash, verify_password, create_access_token


class AuthService:
    """Async authentication service using Beanie ODM."""

    @staticmethod
    async def generate_otp(email: str) -> str:
        """Generate and store OTP for email verification."""
        code = f"{random.randint(100000, 999999)}"
        expires = datetime.utcnow() + timedelta(minutes=10)
        otp = models.EmailOTP(email=email, code=code, expires_at=expires)
        await otp.create()
        return code

    @staticmethod
    async def verify_otp(email: str, code: str) -> Optional[models.EmailOTP]:
        """Verify OTP and return the OTP record if valid."""
        otp = (
            await models.EmailOTP.find(
                models.EmailOTP.email == email,
                models.EmailOTP.code == code,
                models.EmailOTP.used == False,
            )
            .sort(-models.EmailOTP.expires_at)
            .first_or_none()
        )
        if otp and otp.is_valid():
            return otp
        return None

    @staticmethod
    async def mark_otp_used(otp: models.EmailOTP) -> None:
        """Mark OTP as used."""
        otp.used = True
        await otp.save()

    @staticmethod
    async def create_user(email: str, name: str, password: str) -> models.User:
        """Create a new user with hashed password."""
        hashed = get_password_hash(password)
        user = models.User(email=email, name=name, password_hash=hashed)
        await user.create()
        return user

    @staticmethod
    async def authenticate_user(email: str, password: str) -> Optional[models.User]:
        """Authenticate user by email and password."""
        user = await models.User.find_one(models.User.email == email)
        if user and verify_password(password, user.password_hash):
            return user
        return None

    @staticmethod
    def create_token(user_id: str) -> str:
        """Create JWT access token for user."""
        return create_access_token({"sub": user_id})

    @staticmethod
    async def get_user_by_email(email: str) -> Optional[models.User]:
        """Get user by email."""
        return await models.User.find_one(models.User.email == email)

    @staticmethod
    async def get_user_by_id(user_id: str) -> Optional[models.User]:
        """Get user by ID."""
        from uuid import UUID

        try:
            uuid_obj = UUID(user_id)
            return await models.User.get(uuid_obj)
        except ValueError:
            return None
