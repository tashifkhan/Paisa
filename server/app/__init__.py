"""
Paisa API - FastAPI Application
"""

from fastapi import FastAPI
from fastapi.staticfiles import StaticFiles
from fastapi.middleware.cors import CORSMiddleware
from contextlib import asynccontextmanager
import os

from .core.database import init_db
from .core.logger import logger
from .routes import auth, users, expenses, wallets, groups, stats, sync


@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup
    await init_db()
    logger.info("MongoDB initialized")
    yield
    # Shutdown (if needed)


# Create FastAPI app
app = FastAPI(
    title="Paisa API",
    description="Personal finance and expense tracking API",
    version="1.0.0",
    lifespan=lifespan,
)

# Enable CORS for development frontend (adjust in production)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:3000", "http://localhost:5173", "*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Include routers
app.include_router(auth.router)
app.include_router(users.router)
app.include_router(expenses.router)
app.include_router(wallets.router)
app.include_router(groups.router)
app.include_router(stats.router)
app.include_router(sync.router)

# Ensure uploads directory exists
uploads_dir = os.path.join(os.getcwd(), "uploads")
os.makedirs(uploads_dir, exist_ok=True)
logger.info(f"Uploads directory ready: {uploads_dir}")

# Serve uploads/static for simple PWA assets or manifest
app.mount("/uploads", StaticFiles(directory="uploads"), name="uploads")


@app.get("/")
def root():
    logger.debug("Root endpoint accessed")
    return {"message": "Paisa API is running", "docs": "/docs"}


@app.get("/health")
def health_check():
    logger.debug("Health check endpoint accessed")
    return {"status": "healthy"}


__all__ = ["app"]
