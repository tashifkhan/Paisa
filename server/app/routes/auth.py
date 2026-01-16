from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.security import OAuth2PasswordRequestForm
from datetime import datetime, timedelta
import random

from ..models import db_models as models
from ..models import schemas
from ..core.security import get_password_hash, verify_password, create_access_token
from ..core.logger import logger
from ..services.email_service import EmailService

router = APIRouter(prefix="/auth", tags=["auth"])


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

    # create user if password and name provided
    existing = await models.User.find_one(models.User.email == payload.email)
    if existing:
        otp.used = True
        await otp.save()
        raise HTTPException(status_code=400, detail="Email already registered")

    if not payload.password or not payload.name:
        raise HTTPException(
            status_code=400, detail="name and password required to create account"
        )

    hashed = get_password_hash(payload.password)
    user = models.User(email=payload.email, name=payload.name, password_hash=hashed)
    await user.create()

    otp.used = True
    await otp.save()

    token = create_access_token({"sub": str(user.id)})
    return {"access_token": token, "token_type": "bearer", "user_id": str(user.id)}


@router.post("/login", response_model=schemas.Token)
async def login(form_data: OAuth2PasswordRequestForm = Depends()):
    user = await models.User.find_one(models.User.email == form_data.username)
    if not user or not verify_password(form_data.password, user.password_hash):
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
