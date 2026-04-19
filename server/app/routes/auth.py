from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.security import OAuth2PasswordRequestForm
from datetime import datetime, timedelta
import random

from google.oauth2 import id_token
from google.auth.transport import requests as google_requests

from ..models import db_models as models
from ..models import schemas
from ..core.security import get_password_hash, verify_password, create_access_token
from ..core.config import settings
from ..core.logger import logger
from ..services.email_service import EmailService
from ..services.wallet_service import WalletService

router = APIRouter(prefix="/auth", tags=["auth"])

# Default categories to seed for new users
DEFAULT_CATEGORIES = [
    {
        "name": "Food & Dining",
        "icon": "utensils",
        "color": "#FF6B6B",
        "type": "expense",
    },
    {"name": "Transportation", "icon": "car", "color": "#4ECDC4", "type": "expense"},
    {"name": "Shopping", "icon": "shopping-bag", "color": "#45B7D1", "type": "expense"},
    {"name": "Entertainment", "icon": "film", "color": "#96CEB4", "type": "expense"},
    {
        "name": "Bills & Utilities",
        "icon": "file-text",
        "color": "#FFEAA7",
        "type": "expense",
    },
    {"name": "Health", "icon": "heart", "color": "#DDA0DD", "type": "expense"},
    {
        "name": "Groceries",
        "icon": "shopping-cart",
        "color": "#82E0AA",
        "type": "expense",
    },
    {"name": "Other", "icon": "more-horizontal", "color": "#95A5A6", "type": "both"},
    {"name": "Salary", "icon": "briefcase", "color": "#27AE60", "type": "income"},
    {"name": "Freelance", "icon": "laptop", "color": "#3498DB", "type": "income"},
    {"name": "Investment", "icon": "trending-up", "color": "#9B59B6", "type": "income"},
]


async def seed_user_defaults(user_id):
    """Seed default wallet and categories for a new user."""
    # Create default Cash wallet
    await WalletService.seed_default_wallet(user_id)

    # Check if default categories exist (system-wide), if not create user-specific ones
    existing_defaults = await models.Category.find(
        models.Category.is_default == True,
        models.Category.deleted == False,
    ).first_or_none()

    if not existing_defaults:
        # No global defaults, create them as system defaults
        for cat_data in DEFAULT_CATEGORIES:
            cat = models.Category(
                user_id=None,  # System default
                name=cat_data["name"],
                icon=cat_data["icon"],
                color=cat_data["color"],
                type=cat_data["type"],
                is_default=True,
            )
            await cat.create()


@router.post("/request-otp")
async def request_otp(payload: schemas.OTPRequest):
    code = f"{random.randint(100000, 999999)}"
    expires = datetime.utcnow() + timedelta(minutes=10)
    otp = models.EmailOTP(email=payload.email, code=code, expires_at=expires)
    await otp.create()

    # Send OTP via email
    email_sent = await EmailService.send_otp_email(payload.email, code)
    if not email_sent:
        logger.warning(
            f"Failed to send OTP email to {payload.email}, but OTP was created"
        )

    return {"status": "otp_sent"}


@router.post("/verify-otp", response_model=schemas.Token)
async def verify_otp(payload: schemas.OTPVerify):
    otp = (
        await models.EmailOTP.find(
            models.EmailOTP.email == payload.email,
            models.EmailOTP.code == payload.code,
            models.EmailOTP.used == False,
        )
        .sort(-models.EmailOTP.expires_at)
        .first_or_none()
    )

    if not otp or not otp.is_valid():
        raise HTTPException(status_code=400, detail="Invalid or expired OTP")

    # Existing user: either reject signup attempt or reset password
    existing = await models.User.find_one(models.User.email == payload.email)
    if existing:
        if payload.name:
            otp.used = True
            await otp.save()
            raise HTTPException(status_code=400, detail="Email already registered")

        if not payload.password:
            raise HTTPException(
                status_code=400,
                detail="password required for password reset",
            )

        existing.password_hash = get_password_hash(payload.password)
        await existing.save()
        otp.used = True
        await otp.save()

        token = create_access_token({"sub": str(existing.id)})
        return {
            "access_token": token,
            "token_type": "bearer",
            "user_id": str(existing.id),
        }

    if payload.password and not payload.name:
        otp.used = True
        await otp.save()
        raise HTTPException(status_code=400, detail="Email not registered")

    # Signup flow for new users
    if not payload.password or not payload.name:
        raise HTTPException(
            status_code=400, detail="name and password required to create account"
        )

    hashed = get_password_hash(payload.password)
    user = models.User(email=payload.email, name=payload.name, password_hash=hashed)
    await user.create()

    # Seed default wallet and categories for new user
    await seed_user_defaults(user.id)

    otp.used = True
    await otp.save()

    token = create_access_token({"sub": str(user.id)})
    return {"access_token": token, "token_type": "bearer", "user_id": str(user.id)}


@router.post("/login", response_model=schemas.Token)
async def login(form_data: OAuth2PasswordRequestForm = Depends()):
    user = await models.User.find_one(models.User.email == form_data.username)
    if (
        not user
        or not user.password_hash
        or not verify_password(form_data.password, user.password_hash)
    ):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Incorrect email or password",
        )

    access_token = create_access_token({"sub": str(user.id)})
    return {
        "access_token": access_token,
        "token_type": "bearer",
        "user_id": str(user.id),
    }


@router.post("/google", response_model=schemas.Token)
async def google_auth(payload: schemas.GoogleAuthRequest):
    """Authenticate or register user via Google OAuth."""
    try:
        # Verify the Google ID token
        idinfo = id_token.verify_oauth2_token(
            payload.credential, google_requests.Request(), settings.GOOGLE_CLIENT_ID
        )

        email = idinfo.get("email")
        google_id = idinfo.get("sub")
        name = idinfo.get("name")

        if not email:
            raise HTTPException(status_code=400, detail="Email not provided by Google")

        # Check if user exists by email
        user = await models.User.find_one(models.User.email == email)

        if user:
            # Link Google account if not already linked
            if not user.google_id:
                user.google_id = google_id
                await user.save()
                logger.info(f"Linked Google account for existing user: {email}")
        else:
            # Create new user with Google account
            user = models.User(
                email=email,
                name=name,
                google_id=google_id,
                password_hash=None,  # No password for OAuth users
            )
            await user.create()
            logger.info(f"Created new user via Google OAuth: {email}")

            # Seed default wallet and categories for new user
            await seed_user_defaults(user.id)

        token = create_access_token({"sub": str(user.id)})
        return {"access_token": token, "token_type": "bearer", "user_id": str(user.id)}

    except ValueError as e:
        logger.error(f"Google OAuth verification failed: {e}")
        raise HTTPException(status_code=401, detail="Invalid Google token")
