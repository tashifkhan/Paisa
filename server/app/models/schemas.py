from pydantic import BaseModel, EmailStr
from typing import Optional
from datetime import datetime


class UserCreate(BaseModel):
    name: Optional[str]
    email: EmailStr
    password: str


class UserOut(BaseModel):
    id: str
    email: EmailStr
    name: Optional[str]

    class Config:
        orm_mode = True


class UserUpdate(BaseModel):
    name: Optional[str]
    currency: Optional[str]
    language: Optional[str]


class Token(BaseModel):
    access_token: str
    token_type: str
    user_id: str


class OTPRequest(BaseModel):
    email: EmailStr


class OTPVerify(BaseModel):
    email: EmailStr
    code: str
    name: Optional[str]
    password: Optional[str]


class PushTokenUpdate(BaseModel):
    token: str


class TransactionCreate(BaseModel):
    amount: float
    currency: Optional[str] = "INR"
    type: str
    date: Optional[datetime]
    note: Optional[str]
    split_strategy: Optional[str] = "equal"


class TransactionOut(BaseModel):
    id: str
    user_id: str
    wallet_id: Optional[str]
    group_id: Optional[str]
    amount: float
    currency: str
    type: str
    date: Optional[datetime]
    note: Optional[str]

    class Config:
        orm_mode = True


class TransactionUpdate(BaseModel):
    amount: Optional[float]
    currency: Optional[str]
    type: Optional[str]
    date: Optional[datetime]
    note: Optional[str]
    split_strategy: Optional[str]


class WalletCreate(BaseModel):
    name: str
    type: Optional[str] = "personal"
    currency: Optional[str] = "INR"


class WalletOut(BaseModel):
    id: str
    name: str
    type: Optional[str]
    balance: float
    currency: str

    class Config:
        orm_mode = True


class GroupCreate(BaseModel):
    name: str
    base_currency: Optional[str] = "INR"


class GroupOut(BaseModel):
    id: str
    name: str
    base_currency: str
    created_by_user_id: Optional[str]

    class Config:
        orm_mode = True
