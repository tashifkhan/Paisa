"""
Logger configuration for Paisa API
"""

import logging
import sys
from logging.handlers import RotatingFileHandler
from pathlib import Path

import os

# Create logs directory if it doesn't exist
LOGS_DIR = Path("logs")
enable_file_logging = False

# Check if running in serverless environment
IS_SERVERLESS = os.environ.get("SERVERLESS", "false").lower() == "true"

if not IS_SERVERLESS:
    try:
        LOGS_DIR.mkdir(exist_ok=True)
        enable_file_logging = True
    except OSError:
        # Likely read-only file system (e.g., Vercel)
        pass

# Configure logger
logger = logging.getLogger("paisa")
logger.setLevel(logging.DEBUG)

# Create formatters
formatter = logging.Formatter(
    "%(asctime)s - %(name)s - %(levelname)s - %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
)

# Console handler (INFO and above)
console_handler = logging.StreamHandler(sys.stdout)
console_handler.setLevel(logging.INFO)
console_handler.setFormatter(formatter)

# Add console handler
logger.addHandler(console_handler)

# File handler (DEBUG and above, rotating) - only if enabled
if enable_file_logging:
    try:
        file_handler = RotatingFileHandler(
            LOGS_DIR / "paisa.log",
            maxBytes=10 * 1024 * 1024,  # 10MB
            backupCount=5,
        )
        file_handler.setLevel(logging.DEBUG)
        file_handler.setFormatter(formatter)
        logger.addHandler(file_handler)
    except Exception as e:
        print(f"Failed to setup file logging: {e}")

__all__ = ["logger"]
