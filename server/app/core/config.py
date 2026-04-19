from pathlib import Path

from pydantic_settings import BaseSettings, SettingsConfigDict


BASE_DIR = Path(__file__).resolve().parents[2]


class Settings(BaseSettings):
    MONGODB_URL: str = "mongodb://prod:27017/my_app"
    SECRET_KEY: str = "super-secret-prod-key"
    ALGORITHM: str = "HS256"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 60 * 24 * 30  # 30 days

    JWT_ACCESS_SECRET: str = ""
    JWT_REFRESH_SECRET: str = ""

    SMTP_HOST: str | None = ""
    SMTP_PORT: int | None = 587
    SMTP_SECURE: bool = False
    SMTP_USER: str | None = ""
    SMTP_PASS: str | None = ""

    SMTP_FROM: str | None = ""

    GOOGLE_CLIENT_ID: str = ""  # Google OAuth client ID

    model_config = SettingsConfigDict(
        env_file=str(BASE_DIR / ".env"),
        env_file_encoding="utf-8",
        extra="ignore",
    )


settings = Settings()
