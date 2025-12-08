"""
Group service - handles group CRUD operations.
"""

from sqlalchemy.orm import Session
from typing import List

from ..models import db_models as models


class GroupService:
    def __init__(self, db: Session):
        self.db = db

    def create_group(
        self,
        name: str,
        created_by_user_id: str,
        base_currency: str = "INR",
    ) -> models.Group:
        """Create a new group."""
        g = models.Group(
            name=name,
            base_currency=base_currency,
            created_by_user_id=created_by_user_id,
        )
        self.db.add(g)
        self.db.commit()
        self.db.refresh(g)
        return g

    def get_groups(self) -> List[models.Group]:
        """Get all non-deleted groups."""
        return self.db.query(models.Group).filter(models.Group.deleted == False).all()

    def get_group(self, group_id: str) -> models.Group | None:
        """Get a single group by ID."""
        return (
            self.db.query(models.Group)
            .filter(
                models.Group.id == group_id,
                models.Group.deleted == False,
            )
            .first()
        )

    def delete_group(self, group: models.Group) -> None:
        """Soft delete a group."""
        group.deleted = True
        self.db.commit()
