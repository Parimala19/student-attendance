from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    app_name: str = "Attendance API"
    debug: bool = False

    database_url: str = "postgresql+asyncpg://postgres:postgres@localhost:5432/attendance"
    redis_url: str = "redis://localhost:6379/0"

    jwt_secret_key: str = "change-me-in-production"
    jwt_algorithm: str = "HS256"
    access_token_expire_minutes: int = 30
    refresh_token_expire_days: int = 7

    cors_origins: list[str] = ["http://localhost:5173"]

    model_config = {"env_prefix": "ATTENDANCE_", "env_file": ".env"}


settings = Settings()
