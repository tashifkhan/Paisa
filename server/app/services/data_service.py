import csv
import io
import json
from typing import List
from datetime import datetime, date
from uuid import UUID

from app.models.db_models import Transaction
from app.models.schemas import TransactionOut


class DataService:
    @staticmethod
    async def export_transactions_csv(transactions: List[Transaction]) -> str:
        output = io.StringIO()
        writer = csv.writer(output)

        # Headers
        headers = [
            "id",
            "date",
            "amount",
            "currency",
            "type",
            "category",
            "wallet",
            "group",
            "note",
        ]
        writer.writerow(headers)

        for t in transactions:
            # Fetch related names if needed (e.g. category name instead of ID)
            # For MVP, we might just put IDs or simple values.
            # Ideally we'd fetch names, but keeping it simple for now as per plan context.
            # Actually, `transactions` are DB models. we might want to populate them or just dump raw.
            # Let's try to be helpful and dump IDs.

            row = [
                str(t.id),
                t.date.isoformat(),
                t.amount,
                t.currency,
                t.type,
                str(t.category_id) if t.category_id else "",
                str(t.wallet_id) if t.wallet_id else "",
                str(t.group_id) if t.group_id else "",
                t.note or "",
            ]
            writer.writerow(row)

        return output.getvalue()

    @staticmethod
    async def export_transactions_json(transactions: List[Transaction]) -> str:
        # Convert to Pydantic models for easy serialization
        # We can use the TransactionOut schema if it matches what we want to export
        # Or just dict dump

        data = []
        for t in transactions:
            # We preserve UUIDs as strings
            item = t.dict()
            # Convert UUIDs to strings
            for k, v in item.items():
                if isinstance(v, UUID):
                    item[k] = str(v)
                if isinstance(v, (datetime, date)):
                    item[k] = v.isoformat()
            data.append(item)

        return json.dumps(data, indent=2)

    @staticmethod
    async def import_transactions_csv(content: str, user_id: UUID) -> int:
        reader = csv.DictReader(io.StringIO(content))
        transactions_to_create = []

        for row in reader:
            # Basic validation/conversion
            # Assuming headers match export format: id, date, amount, currency, type, category, wallet, group, note

            # Skip empty rows
            if not row or not row.get("amount"):
                continue

            try:
                # We ignore ID on import to always create new records as per plan
                # Unless we wanted to support updates, but plan said "default to creating new records"

                t_date = datetime.fromisoformat(row.get("date"))
                amount = float(row.get("amount"))
                currency = row.get("currency", "INR")
                t_type = row.get("type", "expense")

                # Optional fields
                category_id = UUID(row.get("category")) if row.get("category") else None
                wallet_id = UUID(row.get("wallet")) if row.get("wallet") else None
                group_id = UUID(row.get("group")) if row.get("group") else None
                note = row.get("note")

                transaction = Transaction(
                    user_id=user_id,
                    amount=amount,
                    currency=currency,
                    type=t_type,
                    date=t_date,
                    category_id=category_id,
                    wallet_id=wallet_id,
                    group_id=group_id,
                    note=note,
                )
                transactions_to_create.append(transaction)
            except Exception as e:
                # Skip invalid rows or log them. For MVP, we skip.
                continue

        if transactions_to_create:
            await Transaction.insert_many(transactions_to_create)

        return len(transactions_to_create)

    @staticmethod
    async def import_transactions_json(content: str, user_id: UUID) -> int:
        try:
            data = json.loads(content)
        except json.JSONDecodeError:
            return 0

        if not isinstance(data, list):
            return 0

        transactions_to_create = []
        for item in data:
            try:
                # Same logic as CSV, create new transaction
                t_date = datetime.fromisoformat(item.get("date"))
                amount = float(item.get("amount"))
                currency = item.get("currency", "INR")
                t_type = item.get("type", "expense")

                category_id = (
                    UUID(item.get("category_id")) if item.get("category_id") else None
                )
                wallet_id = (
                    UUID(item.get("wallet_id")) if item.get("wallet_id") else None
                )
                group_id = UUID(item.get("group_id")) if item.get("group_id") else None
                note = item.get("note")

                transaction = Transaction(
                    user_id=user_id,
                    amount=amount,
                    currency=currency,
                    type=t_type,
                    date=t_date,
                    category_id=category_id,
                    wallet_id=wallet_id,
                    group_id=group_id,
                    note=note,
                )
                transactions_to_create.append(transaction)
            except Exception:
                continue

        if transactions_to_create:
            await Transaction.insert_many(transactions_to_create)

        return len(transactions_to_create)
