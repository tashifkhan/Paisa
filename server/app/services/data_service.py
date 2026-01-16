import csv
import io
import json
from typing import List
from datetime import datetime, date
from uuid import UUID

from app.models.db_models import Transaction, Category, Wallet
from app.models.schemas import TransactionOut


class DataService:
    @staticmethod
    async def export_transactions_csv(transactions: List[Transaction]) -> str:
        output = io.StringIO()
        writer = csv.writer(output)

        # Headers - include both ID and name for category/wallet
        headers = [
            "id",
            "date",
            "amount",
            "currency",
            "type",
            "category_id",
            "category_name",
            "wallet_id",
            "wallet_name",
            "group_id",
            "note",
        ]
        writer.writerow(headers)

        # Pre-fetch all categories and wallets for lookup
        all_categories = await Category.find(Category.deleted == False).to_list()
        all_wallets = await Wallet.find(Wallet.deleted == False).to_list()

        cat_map = {c.id: c.name for c in all_categories}
        wallet_map = {w.id: w.name for w in all_wallets}

        for t in transactions:
            cat_name = cat_map.get(t.category_id, "") if t.category_id else ""
            wallet_name = wallet_map.get(t.wallet_id, "") if t.wallet_id else ""

            row = [
                str(t.id),
                t.date.isoformat(),
                t.amount,
                t.currency,
                t.type,
                str(t.category_id) if t.category_id else "",
                cat_name,
                str(t.wallet_id) if t.wallet_id else "",
                wallet_name,
                str(t.group_id) if t.group_id else "",
                t.note or "",
            ]
            writer.writerow(row)

        return output.getvalue()

    @staticmethod
    async def export_transactions_json(transactions: List[Transaction]) -> str:
        # Pre-fetch all categories and wallets for lookup
        all_categories = await Category.find(Category.deleted == False).to_list()
        all_wallets = await Wallet.find(Wallet.deleted == False).to_list()

        cat_map = {c.id: c.name for c in all_categories}
        wallet_map = {w.id: w.name for w in all_wallets}

        data = []
        for t in transactions:
            item = t.dict()
            # Convert UUIDs to strings
            for k, v in item.items():
                if isinstance(v, UUID):
                    item[k] = str(v)
                if isinstance(v, (datetime, date)):
                    item[k] = v.isoformat()

            # Add category and wallet names
            item["category_name"] = (
                cat_map.get(t.category_id, "") if t.category_id else ""
            )
            item["wallet_name"] = wallet_map.get(t.wallet_id, "") if t.wallet_id else ""

            data.append(item)

        return json.dumps(data, indent=2)

    @staticmethod
    async def import_transactions_csv(content: str, user_id: UUID) -> int:
        reader = csv.DictReader(io.StringIO(content))
        transactions_to_create = []

        # Pre-fetch categories and wallets for name-based lookup
        all_categories = await Category.find(Category.deleted == False).to_list()
        all_wallets = await Wallet.find(
            Wallet.user_id == user_id,
            Wallet.deleted == False,
        ).to_list()

        cat_name_map = {c.name.lower(): c.id for c in all_categories}
        wallet_name_map = {w.name.lower(): w.id for w in all_wallets}

        for row in reader:
            # Skip empty rows
            if not row or not row.get("amount"):
                continue

            try:
                t_date = datetime.fromisoformat(row.get("date"))
                amount = float(row.get("amount"))
                currency = row.get("currency", "INR")
                t_type = row.get("type", "expense")

                # Resolve category - try ID first, then name
                category_id = None
                if row.get("category_id"):
                    try:
                        category_id = UUID(row.get("category_id"))
                    except ValueError:
                        pass
                if not category_id and row.get("category_name"):
                    category_id = cat_name_map.get(row.get("category_name", "").lower())
                # Fallback for old format "category" column
                if not category_id and row.get("category"):
                    try:
                        category_id = UUID(row.get("category"))
                    except ValueError:
                        pass

                # Resolve wallet - try ID first, then name
                wallet_id = None
                if row.get("wallet_id"):
                    try:
                        wallet_id = UUID(row.get("wallet_id"))
                    except ValueError:
                        pass
                if not wallet_id and row.get("wallet_name"):
                    wallet_id = wallet_name_map.get(row.get("wallet_name", "").lower())
                # Fallback for old format "wallet" column
                if not wallet_id and row.get("wallet"):
                    try:
                        wallet_id = UUID(row.get("wallet"))
                    except ValueError:
                        pass

                group_id = None
                if row.get("group_id"):
                    try:
                        group_id = UUID(row.get("group_id"))
                    except ValueError:
                        pass
                elif row.get("group"):
                    try:
                        group_id = UUID(row.get("group"))
                    except ValueError:
                        pass

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
                # Skip invalid rows
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

        # Pre-fetch categories and wallets for name-based lookup
        all_categories = await Category.find(Category.deleted == False).to_list()
        all_wallets = await Wallet.find(
            Wallet.user_id == user_id,
            Wallet.deleted == False,
        ).to_list()

        cat_name_map = {c.name.lower(): c.id for c in all_categories}
        wallet_name_map = {w.name.lower(): w.id for w in all_wallets}

        transactions_to_create = []
        for item in data:
            try:
                t_date = datetime.fromisoformat(item.get("date"))
                amount = float(item.get("amount"))
                currency = item.get("currency", "INR")
                t_type = item.get("type", "expense")

                # Resolve category - try ID first, then name
                category_id = None
                if item.get("category_id"):
                    try:
                        category_id = UUID(item.get("category_id"))
                    except ValueError:
                        pass
                if not category_id and item.get("category_name"):
                    category_id = cat_name_map.get(
                        item.get("category_name", "").lower()
                    )

                # Resolve wallet - try ID first, then name
                wallet_id = None
                if item.get("wallet_id"):
                    try:
                        wallet_id = UUID(item.get("wallet_id"))
                    except ValueError:
                        pass
                if not wallet_id and item.get("wallet_name"):
                    wallet_id = wallet_name_map.get(item.get("wallet_name", "").lower())

                group_id = None
                if item.get("group_id"):
                    try:
                        group_id = UUID(item.get("group_id"))
                    except ValueError:
                        pass

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
