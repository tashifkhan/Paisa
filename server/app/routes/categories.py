from fastapi import APIRouter, Depends, HTTPException
from typing import List
from uuid import UUID
from datetime import datetime

from ..core.deps import get_current_user
from ..models import db_models as models
from ..models import schemas

router = APIRouter(prefix="/categories", tags=["categories"])

# Default categories to seed
DEFAULT_CATEGORIES = [
    {
        "name": "Food & Dining",
        "icon": "utensils",
        "color": "#FF6B6B",
        "type": "expense",
    },
    {"name": "Transportation", "icon": "car", "color": "#4ECDC4", "type": "expense"},
    {"name": "Shopping", "icon": "shopping-bag", "color": "#45B7D1", "type": "expense"},
    {"name": "Entertainment", "icon": "film", "color": "#96CEB4", "type": "expense"},
    {
        "name": "Bills & Utilities",
        "icon": "file-text",
        "color": "#FFEAA7",
        "type": "expense",
    },
    {"name": "Health", "icon": "heart", "color": "#DDA0DD", "type": "expense"},
    {"name": "Travel", "icon": "plane", "color": "#98D8C8", "type": "expense"},
    {"name": "Education", "icon": "book", "color": "#F7DC6F", "type": "expense"},
    {
        "name": "Groceries",
        "icon": "shopping-cart",
        "color": "#82E0AA",
        "type": "expense",
    },
    {"name": "Personal Care", "icon": "user", "color": "#F8B500", "type": "expense"},
    {"name": "Gifts", "icon": "gift", "color": "#E74C3C", "type": "expense"},
    {"name": "Other", "icon": "more-horizontal", "color": "#95A5A6", "type": "both"},
    {"name": "Salary", "icon": "briefcase", "color": "#27AE60", "type": "income"},
    {"name": "Freelance", "icon": "laptop", "color": "#3498DB", "type": "income"},
    {"name": "Investment", "icon": "trending-up", "color": "#9B59B6", "type": "income"},
    {"name": "Refund", "icon": "rotate-ccw", "color": "#1ABC9C", "type": "income"},
]


@router.get("/", response_model=List[schemas.CategoryOut])
async def list_categories(current_user=Depends(get_current_user)):
    """
    List all categories (user's custom + system defaults).
    """
    # Get user's custom categories
    user_cats = await models.Category.find(
        models.Category.user_id == current_user.id,
        models.Category.deleted == False,
    ).to_list()

    # Get system default categories
    default_cats = await models.Category.find(
        models.Category.user_id == None,
        models.Category.is_default == True,
        models.Category.deleted == False,
    ).to_list()

    return user_cats + default_cats


@router.get("/defaults", response_model=List[schemas.CategoryOut])
async def list_default_categories():
    """List only system default categories."""
    return await models.Category.find(
        models.Category.is_default == True,
        models.Category.deleted == False,
    ).to_list()


@router.post("/seed-defaults")
async def seed_default_categories():
    """
    Seed the database with default categories.
    This should be run once during initial setup.
    """
    created = 0
    for cat_data in DEFAULT_CATEGORIES:
        # Check if already exists
        existing = await models.Category.find_one(
            models.Category.name == cat_data["name"],
            models.Category.is_default == True,
        )
        if not existing:
            cat = models.Category(
                user_id=None,
                name=cat_data["name"],
                icon=cat_data["icon"],
                color=cat_data["color"],
                type=cat_data["type"],
                is_default=True,
            )
            await cat.create()
            created += 1

    return {"status": "seeded", "created": created, "total": len(DEFAULT_CATEGORIES)}


@router.post("/", response_model=schemas.CategoryOut)
async def create_category(
    payload: schemas.CategoryCreate,
    current_user=Depends(get_current_user),
):
    """Create a custom category for the user."""
    cat = models.Category(
        user_id=current_user.id,
        name=payload.name,
        icon=payload.icon,
        color=payload.color,
        type=payload.type,
        is_default=False,
    )
    await cat.create()
    return cat


@router.get("/{category_id}", response_model=schemas.CategoryOut)
async def get_category(category_id: str, current_user=Depends(get_current_user)):
    """Get a single category by ID."""
    try:
        uuid_obj = UUID(category_id)
    except ValueError:
        raise HTTPException(status_code=404, detail="Invalid UUID")

    cat = await models.Category.find_one(
        models.Category.id == uuid_obj,
        models.Category.deleted == False,
    )
    if not cat:
        raise HTTPException(status_code=404, detail="Category not found")

    # Check access: either user's own category or a default one
    if cat.user_id and cat.user_id != current_user.id:
        raise HTTPException(status_code=403, detail="Access denied")

    return cat


@router.put("/{category_id}", response_model=schemas.CategoryOut)
async def update_category(
    category_id: str,
    payload: schemas.CategoryUpdate,
    current_user=Depends(get_current_user),
):
    """Update a user's custom category."""
    try:
        uuid_obj = UUID(category_id)
    except ValueError:
        raise HTTPException(status_code=404, detail="Invalid UUID")

    cat = await models.Category.find_one(
        models.Category.id == uuid_obj,
        models.Category.user_id == current_user.id,
        models.Category.deleted == False,
    )
    if not cat:
        raise HTTPException(
            status_code=404,
            detail="Category not found or cannot modify default categories",
        )

    if payload.name is not None:
        cat.name = payload.name
    if payload.icon is not None:
        cat.icon = payload.icon
    if payload.color is not None:
        cat.color = payload.color
    if payload.type is not None:
        cat.type = payload.type

    await cat.save()
    return cat


@router.delete("/{category_id}")
async def delete_category(category_id: str, current_user=Depends(get_current_user)):
    """Delete a user's custom category."""
    try:
        uuid_obj = UUID(category_id)
    except ValueError:
        raise HTTPException(status_code=404, detail="Invalid UUID")

    cat = await models.Category.find_one(
        models.Category.id == uuid_obj,
        models.Category.user_id == current_user.id,
    )
    if not cat:
        raise HTTPException(
            status_code=404,
            detail="Category not found or cannot delete default categories",
        )

    cat.deleted = True
    await cat.save()
    return {"status": "deleted"}
