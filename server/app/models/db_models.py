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
    password_hash: str
    name: Optional[str] = None
    currency: str = "INR"
    language: str = "en"
    device_token: Optional[str] = None
    created_at: datetime = Field(default_factory=datetime.utcnow)

    class Settings:
        name = "users"


class Wallet(Document):
    id: UUID = Field(default_factory=uuid4)
    user_id: UUID  # Reference to User.id
    name: str
    type: str
    balance: float = 0.0
    currency: str = "INR"
    updated_at: datetime = Field(default_factory=datetime.utcnow)
    deleted: bool = False

    class Settings:
        name = "wallets"


class Group(Document):
    id: UUID = Field(default_factory=uuid4)
    name: str
    base_currency: str = "INR"
    created_by_user_id: UUID
    updated_at: datetime = Field(default_factory=datetime.utcnow)
    deleted: bool = False

    class Settings:
        name = "groups"


class Transaction(Document):
    id: UUID = Field(default_factory=uuid4)
    user_id: UUID
    wallet_id: Optional[UUID] = None
    group_id: Optional[UUID] = None

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
