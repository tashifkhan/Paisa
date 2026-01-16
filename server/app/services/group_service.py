"""
Group service - handles group CRUD operations and member management.
Async/Beanie implementation.
"""

from typing import List, Optional
from uuid import UUID
from datetime import datetime

from ..models import db_models as models


class GroupService:
    """Async group service using Beanie ODM."""

    @staticmethod
    async def create_group(
        name: str,
        created_by_user_id: UUID,
        base_currency: str = "INR",
        icon: Optional[str] = None,
        color: Optional[str] = None,
    ) -> models.Group:
        """Create a new group and add creator as admin member."""
        g = models.Group(
            name=name,
            base_currency=base_currency,
            created_by_user_id=created_by_user_id,
            icon=icon,
            color=color,
        )
        await g.create()

        # Add creator as admin member
        member = models.GroupMember(
            group_id=g.id,
            user_id=created_by_user_id,
            role="admin",
        )
        await member.create()

        return g

    @staticmethod
    async def get_groups(user_id: UUID) -> List[models.Group]:
        """Get all non-deleted groups that user is a member of."""
        # First get all group memberships for this user
        memberships = await models.GroupMember.find(
            {"user_id": user_id, "deleted": False}
        ).to_list()

        group_ids = [m.group_id for m in memberships]

        if not group_ids:
            return []

        # Then get the groups
        return await models.Group.find(
            {"id": {"$in": group_ids}},
            models.Group.deleted == False,
        ).to_list()

    @staticmethod
    async def get_group(group_id: UUID) -> Optional[models.Group]:
        """Get a single group by ID."""
        return await models.Group.find_one(
            models.Group.id == group_id,
            models.Group.deleted == False,
        )

    @staticmethod
    async def update_group(
        group: models.Group,
        name: Optional[str] = None,
        base_currency: Optional[str] = None,
        icon: Optional[str] = None,
        color: Optional[str] = None,
    ) -> models.Group:
        """Update group fields."""
        if name is not None:
            group.name = name
        if base_currency is not None:
            group.base_currency = base_currency
        if icon is not None:
            group.icon = icon
        if color is not None:
            group.color = color
        group.updated_at = datetime.utcnow()
        await group.save()
        return group

    @staticmethod
    async def delete_group(group: models.Group) -> None:
        """Soft delete a group."""
        group.deleted = True
        group.updated_at = datetime.utcnow()
        await group.save()

    # Member Management
    @staticmethod
    async def add_member(
        group_id: UUID,
        user_id: UUID,
        role: str = "member",
    ) -> models.GroupMember:
        """Add a user to a group."""
        # Check if already a member
        existing = await models.GroupMember.find_one(
            models.GroupMember.group_id == group_id,
            models.GroupMember.user_id == user_id,
        )
        if existing:
            if existing.deleted:
                # Reactivate membership
                existing.deleted = False
                existing.role = role
                existing.joined_at = datetime.utcnow()
                await existing.save()
                return existing
            return existing

        member = models.GroupMember(
            group_id=group_id,
            user_id=user_id,
            role=role,
        )
        await member.create()
        return member

    @staticmethod
    async def remove_member(group_id: UUID, user_id: UUID) -> bool:
        """Remove a user from a group (soft delete)."""
        member = await models.GroupMember.find_one(
            models.GroupMember.group_id == group_id,
            models.GroupMember.user_id == user_id,
            models.GroupMember.deleted == False,
        )
        if member:
            member.deleted = True
            await member.save()
            return True
        return False

    @staticmethod
    async def get_members(group_id: UUID) -> List[models.GroupMember]:
        """Get all active members of a group."""
        return await models.GroupMember.find(
            models.GroupMember.group_id == group_id,
            models.GroupMember.deleted == False,
        ).to_list()

    @staticmethod
    async def get_member_count(group_id: UUID) -> int:
        """Get count of active members in a group."""
        return await models.GroupMember.find(
            models.GroupMember.group_id == group_id,
            models.GroupMember.deleted == False,
        ).count()

    @staticmethod
    async def is_member(group_id: UUID, user_id: UUID) -> bool:
        """Check if user is a member of the group."""
        member = await models.GroupMember.find_one(
            models.GroupMember.group_id == group_id,
            models.GroupMember.user_id == user_id,
            models.GroupMember.deleted == False,
        )
        return member is not None

    @staticmethod
    async def is_admin(group_id: UUID, user_id: UUID) -> bool:
        """Check if user is an admin of the group."""
        member = await models.GroupMember.find_one(
            models.GroupMember.group_id == group_id,
            models.GroupMember.user_id == user_id,
            models.GroupMember.role == "admin",
            models.GroupMember.deleted == False,
        )
        return member is not None

    @staticmethod
    async def get_group_expenses(group_id: UUID) -> List[models.Transaction]:
        """Get all expenses for a group."""
        return (
            await models.Transaction.find(
                models.Transaction.group_id == group_id,
                models.Transaction.deleted == False,
            )
            .sort(-models.Transaction.date)
            .to_list()
        )

    @staticmethod
    async def get_group_total_expenses(group_id: UUID) -> float:
        """Get total expenses for a group."""
        txns = await models.Transaction.find(
            models.Transaction.group_id == group_id,
            models.Transaction.type == "expense",
            models.Transaction.deleted == False,
        ).to_list()
        return sum(t.amount for t in txns)

    @staticmethod
    async def calculate_group_balances(
        group_id: UUID,
    ) -> dict[UUID, float]:
        """
        Calculate balance for each member in a group.
        Positive = they are owed money, Negative = they owe money.
        """
        # Get all group expenses
        expenses = await models.Transaction.find(
            models.Transaction.group_id == group_id,
            models.Transaction.type == "expense",
            models.Transaction.deleted == False,
        ).to_list()

        # Get all members
        members = await GroupService.get_members(group_id)
        member_ids = [m.user_id for m in members]
        member_count = len(member_ids)

        if member_count == 0:
            return {}

        # Initialize balances
        balances: dict[UUID, float] = {uid: 0.0 for uid in member_ids}

        for expense in expenses:
            # Who paid
            payer_id = expense.user_id
            amount = expense.amount

            if expense.splits:
                # Use splits if available
                for split in expense.splits:
                    if split.user_id and split.user_id in balances:
                        balances[split.user_id] -= split.amount_owed
                # Credit the payer
                if payer_id in balances:
                    balances[payer_id] += amount
            else:
                # Equal split among all members
                share = amount / member_count
                for uid in member_ids:
                    balances[uid] -= share
                # Credit the payer
                if payer_id in balances:
                    balances[payer_id] += amount

        return balances
