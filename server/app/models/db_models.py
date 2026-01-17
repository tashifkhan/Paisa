from typing import List, Optional, Literal
from datetime import datetime
from uuid import UUID, uuid4
from beanie import Document, Link, Indexed
from pydantic import BaseModel, Field


# Using Pydantic models for embedded documents
class TransactionSplit(BaseModel):
    user_id: Optional[UUID] = None
    amount_owed: float
    percentage: Optional[float] = None
    shares: Optional[float] = None
    updated_at: datetime = Field(default_factory=datetime.utcnow)


class User(Document):
    id: UUID = Field(default_factory=uuid4)
    email: Indexed(str, unique=True)
    password_hash: Optional[str] = None  # Optional for OAuth users
    google_id: Optional[str] = None  # Google OAuth user ID
    name: Optional[str] = None
    currency: str = "INR"
    language: str = "en"
    device_token: Optional[str] = None
    created_at: datetime = Field(default_factory=datetime.utcnow)
    last_synced_at: Optional[datetime] = None

    class Settings:
        name = "users"


class Category(Document):
    """Expense/Income categories for organizing transactions."""

    id: UUID = Field(default_factory=uuid4)
    user_id: Optional[UUID] = None  # None = system default category
    name: str
    icon: Optional[str] = None  # Icon name/identifier
    color: Optional[str] = None  # Hex color code
    type: Literal["expense", "income", "both"] = "expense"
    is_default: bool = False
    created_at: datetime = Field(default_factory=datetime.utcnow)
    deleted: bool = False

    class Settings:
        name = "categories"


class Wallet(Document):
    id: UUID = Field(default_factory=uuid4)
    user_id: UUID  # Reference to User.id
    name: str
    type: str
    balance: float = 0.0
    currency: str = "INR"
    updated_at: datetime = Field(default_factory=datetime.utcnow)
    last_synced_at: Optional[datetime] = None
    deleted: bool = False

    class Settings:
        name = "wallets"


class Group(Document):
    id: UUID = Field(default_factory=uuid4)
    name: str
    base_currency: str = "INR"
    created_by_user_id: UUID
    icon: Optional[str] = None
    color: Optional[str] = None
    updated_at: datetime = Field(default_factory=datetime.utcnow)
    last_synced_at: Optional[datetime] = None
    deleted: bool = False

    class Settings:
        name = "groups"


class GroupMember(Document):
    """Tracks membership in groups."""

    id: UUID = Field(default_factory=uuid4)
    group_id: UUID
    user_id: UUID
    role: Literal["admin", "member"] = "member"
    joined_at: datetime = Field(default_factory=datetime.utcnow)
    deleted: bool = False

    class Settings:
        name = "group_members"


class Transaction(Document):
    id: UUID = Field(default_factory=uuid4)
    user_id: UUID
    wallet_id: Optional[UUID] = None
    group_id: Optional[UUID] = None
    category_id: Optional[UUID] = None

    amount: float
    currency: str = "INR"
    exchange_rate: float = 1.0

    type: Literal[
        "expense", "income", "transfer"
    ]  # Added transfer for completeness, though original schema just had String
    split_strategy: str = "equal"

    bill_image_url: Optional[str] = None

    is_recurring: bool = False
    recurrence_rule: Optional[str] = None

    date: datetime
    note: Optional[str] = None
    updated_at: datetime = Field(default_factory=datetime.utcnow)
    last_synced_at: Optional[datetime] = None
    deleted: bool = False

    # Embedded splits
    splits: List[TransactionSplit] = []

    class Settings:
        name = "transactions"


class EmailOTP(Document):
    id: UUID = Field(default_factory=uuid4)
    email: Indexed(str)
    code: str
    expires_at: datetime
    used: bool = False

    def is_valid(self):
        return (not self.used) and (
            self.expires_at and self.expires_at > datetime.utcnow()
        )

    class Settings:
        name = "email_otps"


class Debt(Document):
    id: UUID = Field(default_factory=uuid4)
    user_id: UUID  # The user who owns this record (the "me" in "owed to me")
    counterparty_name: str
    amount: float
    type: Literal["owed_to_me", "owed_by_me"]
    due_date: Optional[str] = None  # simplified for UI "Due in 3 days"
    updated_at: datetime = Field(default_factory=datetime.utcnow)
    last_synced_at: Optional[datetime] = None
    deleted: bool = False

    class Settings:
        name = "debts"
