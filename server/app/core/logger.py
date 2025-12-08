"""
Logger configuration for Paisa API
"""

import logging
import sys
from logging.handlers import RotatingFileHandler
from pathlib import Path

# Create logs directory if it doesn't exist
LOGS_DIR = Path("logs")
LOGS_DIR.mkdir(exist_ok=True)

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

# File handler (DEBUG and above, rotating)
file_handler = RotatingFileHandler(
    LOGS_DIR / "paisa.log",
    maxBytes=10 * 1024 * 1024,  # 10MB
    backupCount=5,
)
file_handler.setLevel(logging.DEBUG)
file_handler.setFormatter(formatter)

# Add handlers
logger.addHandler(console_handler)
logger.addHandler(file_handler)

__all__ = ["logger"]
