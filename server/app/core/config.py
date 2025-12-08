from pydantic_settings import BaseSettings


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

    class Config:
        env_file = ".env"


settings = Settings()
