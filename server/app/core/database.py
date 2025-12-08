from motor.motor_asyncio import AsyncIOMotorClient
from beanie import init_beanie
from .config import settings

# Import all models here so Beanie can register them
from ..models.db_models import User, Wallet, Group, Transaction, EmailOTP


async def init_db():
    client = AsyncIOMotorClient(settings.MONGODB_URL)
    await init_beanie(
        database=client.get_default_database(),
        document_models=[User, Wallet, Group, Transaction, EmailOTP],
    )
