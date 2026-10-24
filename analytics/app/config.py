from pathlib import Path
from pydantic import AliasChoices, Field
from pydantic_settings import BaseSettings, SettingsConfigDict

# Robust path resolution independent of process working directory
PROJECT_ROOT = Path(__file__).resolve().parent.parent.parent
ROOT_ENV_FILE = PROJECT_ROOT / ".env"
ANALYTICS_ENV_FILE = Path(__file__).resolve().parent.parent / ".env"

class Settings(BaseSettings):
    GEMINI_API_KEY: str = Field(
        default="",
        validation_alias=AliasChoices("GEMINI_API_KEY", "AI_API_KEY"),
    )
    GEMINI_MODEL: str = "gemini-3-flash-preview"
    GEMINI_TIMEOUT_SECONDS: int = 20

    model_config = SettingsConfigDict(
        env_file=(ROOT_ENV_FILE, ANALYTICS_ENV_FILE),
        env_file_encoding="utf-8",
        extra="ignore",
    )

settings = Settings()

