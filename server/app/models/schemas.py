from pydantic import BaseModel, EmailStr, model_validator
from typing import Optional, Literal, List, Any
from datetime import datetime
from uuid import UUID


# Split Schemas (embedded in transactions)
class SplitCreate(BaseModel):
    user_id: str
    amount_owed: Optional[float] = None
    percentage: Optional[float] = None
    shares: Optional[float] = None


class SplitOut(BaseModel):
    user_id: str
    amount_owed: float
    percentage: Optional[float] = None
    shares: Optional[float] = None


def convert_uuid_to_str(v: Any) -> Any:
    """Convert UUID to string."""
    if isinstance(v, UUID):
        return str(v)
    return v


class BaseOutModel(BaseModel):
    """Base model for all output schemas with UUID conversion support."""

    @model_validator(mode="before")
    @classmethod
    def convert_uuids(cls, data: Any) -> Any:
        if hasattr(data, "__dict__") and not isinstance(data, dict):
            # It's an ORM model, convert to dict
            result = {}
            for field in cls.model_fields:
                val = getattr(data, field, None)
                result[field] = convert_uuid_to_str(val)
            return result
        elif isinstance(data, dict):
            return {k: convert_uuid_to_str(v) for k, v in data.items()}
        return data

    class Config:
        from_attributes = True


class UserCreate(BaseModel):
    name: Optional[str]
    email: EmailStr
    password: str


class UserOut(BaseOutModel):
    id: str
    email: EmailStr
    name: Optional[str]
    currency: Optional[str] = "INR"
    language: Optional[str] = "en"


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


class GoogleAuthRequest(BaseModel):
    credential: str  # Google ID token from frontend


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


class CategoryOut(BaseOutModel):
    id: str
    name: str
    icon: Optional[str]
    color: Optional[str]
    type: str
    is_default: bool


# Transaction Schemas
class TransactionCreate(BaseModel):
    amount: float
    currency: Optional[str] = "INR"
    type: Optional[Literal["expense", "income", "transfer"]] = "expense"
    date: Optional[datetime] = None
    note: Optional[str] = None
    title: Optional[str] = None
    description: Optional[str] = None
    split_strategy: Optional[str] = "equal"
    wallet_id: Optional[str] = None
    to_wallet_id: Optional[str] = None
    group_id: Optional[str] = None
    category_id: Optional[str] = None
    bill_image_url: Optional[str] = None
    splits: Optional[List[SplitCreate]] = None


class TransactionOut(BaseOutModel):
    id: str
    user_id: str
    wallet_id: Optional[str]
    to_wallet_id: Optional[str]
    group_id: Optional[str]
    category_id: Optional[str]
    amount: float
    currency: str
    type: str
    date: Optional[datetime]
    note: Optional[str]
    title: Optional[str]
    description: Optional[str]
    bill_image_url: Optional[str]
    split_strategy: Optional[str]
    splits: List[SplitOut] = []


class TransactionUpdate(BaseModel):
    amount: Optional[float] = None
    currency: Optional[str] = None
    type: Optional[Literal["expense", "income", "transfer"]] = None
    date: Optional[datetime] = None
    note: Optional[str] = None
    title: Optional[str] = None
    description: Optional[str] = None
    split_strategy: Optional[str] = None
    wallet_id: Optional[str] = None
    to_wallet_id: Optional[str] = None
    category_id: Optional[str] = None
    bill_image_url: Optional[str] = None
    splits: Optional[List[SplitCreate]] = None


# Wallet Schemas
class WalletCreate(BaseModel):
    name: str
    type: Optional[str] = "personal"
    currency: Optional[str] = "INR"
    credit_limit: Optional[float] = None
    statement_day: Optional[int] = None
    due_day: Optional[int] = None


class WalletUpdate(BaseModel):
    name: Optional[str] = None
    type: Optional[str] = None
    currency: Optional[str] = None
    credit_limit: Optional[float] = None
    statement_day: Optional[int] = None
    due_day: Optional[int] = None


class WalletOut(BaseOutModel):
    id: str
    name: str
    type: Optional[str]
    balance: float
    currency: str
    credit_limit: Optional[float] = None
    available_credit: Optional[float] = None
    utilization_percent: Optional[float] = None
    statement_day: Optional[int] = None
    due_day: Optional[int] = None


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


class GroupOut(BaseOutModel):
    id: str
    name: str
    base_currency: str
    created_by_user_id: Optional[str]
    icon: Optional[str]
    color: Optional[str]


# Group Member Schemas
class GroupMemberCreate(BaseModel):
    user_id: str
    role: Literal["admin", "member"] = "member"


class GroupMemberOut(BaseOutModel):
    id: str
    group_id: str
    user_id: str
    role: str
    joined_at: datetime


class GroupMemberWithUser(BaseOutModel):
    id: str
    user_id: str
    role: str
    joined_at: datetime
    user_name: Optional[str]
    user_email: Optional[str]


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


class DebtOut(BaseOutModel):
    id: str
    counterparty_name: str
    amount: float
    type: str
    due_date: Optional[str]


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


# Simplify Debts Schemas
class SimplifiedDebt(BaseModel):
    from_user_id: str
    from_user_name: str
    to_user_id: str
    to_user_name: str
    amount: float


class SimplifyDebtsResponse(BaseModel):
    group_id: str
    group_name: str
    simplified_debts: List[SimplifiedDebt]
    total_transactions: int
