from pydantic import BaseModel, EmailStr
from typing import Optional, Literal, List
from datetime import datetime


class UserCreate(BaseModel):
    name: Optional[str]
    email: EmailStr
    password: str


class UserOut(BaseModel):
    id: str
    email: EmailStr
    name: Optional[str]
    currency: Optional[str] = "INR"
    language: Optional[str] = "en"

    class Config:
        from_attributes = True


class UserUpdate(BaseModel):
    name: Optional[str] = None
    currency: Optional[str] = None
    language: Optional[str] = None


class Token(BaseModel):
    access_token: str
    token_type: str
    user_id: str


class OTPRequest(BaseModel):
    email: EmailStr


class OTPVerify(BaseModel):
    email: EmailStr
    code: str
    name: Optional[str] = None
    password: Optional[str] = None


class PushTokenUpdate(BaseModel):
    token: str


# Category Schemas
class CategoryCreate(BaseModel):
    name: str
    icon: Optional[str] = None
    color: Optional[str] = None
    type: Literal["expense", "income", "both"] = "expense"


class CategoryUpdate(BaseModel):
    name: Optional[str] = None
    icon: Optional[str] = None
    color: Optional[str] = None
    type: Optional[Literal["expense", "income", "both"]] = None


class CategoryOut(BaseModel):
    id: str
    name: str
    icon: Optional[str]
    color: Optional[str]
    type: str
    is_default: bool

    class Config:
        from_attributes = True


# Transaction Schemas
class TransactionCreate(BaseModel):
    amount: float
    currency: Optional[str] = "INR"
    type: str
    date: Optional[datetime] = None
    note: Optional[str] = None
    split_strategy: Optional[str] = "equal"
    wallet_id: Optional[str] = None
    group_id: Optional[str] = None
    category_id: Optional[str] = None


class TransactionOut(BaseModel):
    id: str
    user_id: str
    wallet_id: Optional[str]
    group_id: Optional[str]
    category_id: Optional[str]
    amount: float
    currency: str
    type: str
    date: Optional[datetime]
    note: Optional[str]

    class Config:
        from_attributes = True


class TransactionUpdate(BaseModel):
    amount: Optional[float] = None
    currency: Optional[str] = None
    type: Optional[str] = None
    date: Optional[datetime] = None
    note: Optional[str] = None
    split_strategy: Optional[str] = None
    wallet_id: Optional[str] = None
    category_id: Optional[str] = None


# Wallet Schemas
class WalletCreate(BaseModel):
    name: str
    type: Optional[str] = "personal"
    currency: Optional[str] = "INR"


class WalletUpdate(BaseModel):
    name: Optional[str] = None
    type: Optional[str] = None
    currency: Optional[str] = None


class WalletOut(BaseModel):
    id: str
    name: str
    type: Optional[str]
    balance: float
    currency: str

    class Config:
        from_attributes = True


# Group Schemas
class GroupCreate(BaseModel):
    name: str
    base_currency: Optional[str] = "INR"
    icon: Optional[str] = None
    color: Optional[str] = None


class GroupUpdate(BaseModel):
    name: Optional[str] = None
    base_currency: Optional[str] = None
    icon: Optional[str] = None
    color: Optional[str] = None


class GroupOut(BaseModel):
    id: str
    name: str
    base_currency: str
    created_by_user_id: Optional[str]
    icon: Optional[str]
    color: Optional[str]

    class Config:
        from_attributes = True


# Group Member Schemas
class GroupMemberCreate(BaseModel):
    user_id: str
    role: Literal["admin", "member"] = "member"


class GroupMemberOut(BaseModel):
    id: str
    group_id: str
    user_id: str
    role: str
    joined_at: datetime

    class Config:
        from_attributes = True


class GroupMemberWithUser(BaseModel):
    id: str
    user_id: str
    role: str
    joined_at: datetime
    user_name: Optional[str]
    user_email: Optional[str]

    class Config:
        from_attributes = True


# Group Balance Schemas
class UserBalance(BaseModel):
    user_id: str
    user_name: Optional[str]
    balance: float  # positive = owed to them, negative = they owe


class GroupBalanceSummary(BaseModel):
    group_id: str
    group_name: str
    total_expenses: float
    balances: List[UserBalance]


# Debt Schemas
class DebtCreate(BaseModel):
    counterparty_name: str
    amount: float
    type: Literal["owed_to_me", "owed_by_me"]
    due_date: Optional[str] = None


class DebtUpdate(BaseModel):
    counterparty_name: Optional[str] = None
    amount: Optional[float] = None
    type: Optional[Literal["owed_to_me", "owed_by_me"]] = None
    due_date: Optional[str] = None


class DebtOut(BaseModel):
    id: str
    counterparty_name: str
    amount: float
    type: str
    due_date: Optional[str]

    class Config:
        from_attributes = True


# Stats Schemas
class StatsSummary(BaseModel):
    period_days: int
    totals: dict


class CategoryStats(BaseModel):
    category_id: Optional[str]
    category_name: str
    total: float
    percentage: float
    count: int


class StatsBreakdown(BaseModel):
    period_days: int
    total_expense: float
    total_income: float
    net: float
    by_category: List[CategoryStats]
