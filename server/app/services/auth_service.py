"""
Authentication service - handles OTP, login, and token creation logic.
"""

from sqlalchemy.orm import Session
from datetime import datetime, timedelta
import random

from ..models import db_models as models
from ..core.security import get_password_hash, verify_password, create_access_token


class AuthService:
    def __init__(self, db: Session):
        self.db = db

    def generate_otp(self, email: str) -> str:
        """Generate and store OTP for email verification."""
        code = f"{random.randint(100000, 999999)}"
        expires = datetime.utcnow() + timedelta(minutes=10)
        otp = models.EmailOTP(email=email, code=code, expires_at=expires)
        self.db.add(otp)
        self.db.commit()
        return code

    def verify_otp(self, email: str, code: str) -> models.EmailOTP | None:
        """Verify OTP and return the OTP record if valid."""
        otp = (
            self.db.query(models.EmailOTP)
            .filter(
                models.EmailOTP.email == email,
                models.EmailOTP.code == code,
                models.EmailOTP.used == False,
            )
            .order_by(models.EmailOTP.expires_at.desc())
            .first()
        )
        if otp and otp.is_valid():
            return otp
        return None

    def create_user(self, email: str, name: str, password: str) -> models.User:
        """Create a new user with hashed password."""
        hashed = get_password_hash(password)
        user = models.User(email=email, name=name, password_hash=hashed)
        self.db.add(user)
        self.db.commit()
        self.db.refresh(user)
        return user

    def authenticate_user(self, email: str, password: str) -> models.User | None:
        """Authenticate user by email and password."""
        user = self.db.query(models.User).filter(models.User.email == email).first()
        if user and verify_password(password, user.password_hash):
            return user
        return None

    def create_token(self, user_id: str) -> str:
        """Create JWT access token for user."""
        return create_access_token({"sub": user_id})

    def get_user_by_email(self, email: str) -> models.User | None:
        """Get user by email."""
        return self.db.query(models.User).filter(models.User.email == email).first()
