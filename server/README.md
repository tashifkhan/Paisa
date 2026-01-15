# Paisa - FastAPI Backend

A comprehensive personal finance and expense tracking API built with FastAPI and MongoDB.

## Features

- **User Authentication**: OTP-based signup, JWT login
- **Expense Tracking**: Full CRUD for transactions with wallet and category support
- **Wallets**: Multi-wallet support with automatic balance updates
- **Categories**: Built-in expense/income categories with custom category support
- **Groups**: Splitwise-style group expense tracking with balance calculations
- **Debts**: Track money owed to and by you
- **Statistics**: Comprehensive spending analysis and trends
- **Sync**: Mobile-friendly sync endpoint with change tracking

## Quick Start

### 1. Install Dependencies

Ensure you have Python 3.12+ and MongoDB installed.

```bash
uv sync  # using uv
# OR
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

### 2. Configure Environment

Create a `.env` file (optional) or use defaults in `app/core/config.py`.

Key variables:

- `MONGODB_URL` (default: `mongodb://localhost:27017/paisa`)
- `SECRET_KEY`

### 3. Run the Development Server

```bash
uv run python main.py
# OR
uvicorn app:app --reload --host 0.0.0.0 --port 8000
```

### 4. Seed Default Categories (Optional)

```bash
curl -X POST http://localhost:8000/categories/seed-defaults
```

## API Endpoints

### Authentication (`/auth`)

| Method | Endpoint            | Description                                  |
| ------ | ------------------- | -------------------------------------------- |
| POST   | `/auth/request-otp` | Request signup OTP (email logged to console) |
| POST   | `/auth/verify-otp`  | Verify OTP and create user                   |
| POST   | `/auth/login`       | Login with email & password                  |

### Users (`/users`)

| Method | Endpoint            | Description                 |
| ------ | ------------------- | --------------------------- |
| GET    | `/users/me`         | Get current user details    |
| PUT    | `/users/me`         | Update current user profile |
| PUT    | `/users/push-token` | Update device push token    |
| GET    | `/users/{user_id}`  | Get user by ID              |

### Expenses/Transactions (`/expenses`)

| Method | Endpoint                | Description                                                                          |
| ------ | ----------------------- | ------------------------------------------------------------------------------------ |
| GET    | `/expenses/`            | List transactions (with filters: wallet_id, category_id, type, start_date, end_date) |
| POST   | `/expenses/add`         | Add a transaction                                                                    |
| GET    | `/expenses/{txn_id}`    | Get single transaction                                                               |
| PUT    | `/expenses/{txn_id}`    | Update transaction                                                                   |
| DELETE | `/expenses/{txn_id}`    | Delete transaction                                                                   |
| POST   | `/expenses/upload-bill` | Upload bill image                                                                    |
| GET    | `/expenses/export`      | Export transactions as CSV                                                           |

### Wallets (`/wallets`)

| Method | Endpoint                              | Description                      |
| ------ | ------------------------------------- | -------------------------------- |
| GET    | `/wallets/`                           | List user wallets                |
| POST   | `/wallets/`                           | Create a wallet                  |
| GET    | `/wallets/total`                      | Get total balance across wallets |
| GET    | `/wallets/{wallet_id}`                | Get single wallet                |
| PUT    | `/wallets/{wallet_id}`                | Update wallet                    |
| DELETE | `/wallets/{wallet_id}`                | Delete wallet                    |
| GET    | `/wallets/{wallet_id}/transactions`   | Get wallet transactions          |
| POST   | `/wallets/{wallet_id}/adjust-balance` | Manually adjust balance          |

### Categories (`/categories`)

| Method | Endpoint                    | Description                           |
| ------ | --------------------------- | ------------------------------------- |
| GET    | `/categories/`              | List all categories (user + defaults) |
| GET    | `/categories/defaults`      | List default categories only          |
| POST   | `/categories/seed-defaults` | Seed database with default categories |
| POST   | `/categories/`              | Create custom category                |
| GET    | `/categories/{category_id}` | Get single category                   |
| PUT    | `/categories/{category_id}` | Update custom category                |
| DELETE | `/categories/{category_id}` | Delete custom category                |

### Groups (`/groups`)

| Method | Endpoint                               | Description               |
| ------ | -------------------------------------- | ------------------------- |
| GET    | `/groups/`                             | List user's groups        |
| POST   | `/groups/`                             | Create a group            |
| GET    | `/groups/{group_id}`                   | Get single group          |
| PUT    | `/groups/{group_id}`                   | Update group (admin only) |
| DELETE | `/groups/{group_id}`                   | Delete group (admin only) |
| GET    | `/groups/{group_id}/members`           | List group members        |
| POST   | `/groups/{group_id}/members`           | Add member (admin only)   |
| DELETE | `/groups/{group_id}/members/{user_id}` | Remove member             |
| GET    | `/groups/{group_id}/expenses`          | List group expenses       |
| POST   | `/groups/{group_id}/expenses`          | Add group expense         |
| GET    | `/groups/{group_id}/balances`          | Get group balance summary |

### Debts (`/debts`)

| Method | Endpoint                  | Description               |
| ------ | ------------------------- | ------------------------- |
| GET    | `/debts/`                 | List all debts            |
| POST   | `/debts/`                 | Create a debt             |
| GET    | `/debts/summary`          | Get debt summary (totals) |
| GET    | `/debts/owed-to-me`       | List debts owed to you    |
| GET    | `/debts/owed-by-me`       | List debts you owe        |
| GET    | `/debts/{debt_id}`        | Get single debt           |
| PUT    | `/debts/{debt_id}`        | Update debt               |
| DELETE | `/debts/{debt_id}`        | Delete debt               |
| POST   | `/debts/{debt_id}/settle` | Mark debt as settled      |

### Statistics (`/stats`)

| Method | Endpoint            | Description                                     |
| ------ | ------------------- | ----------------------------------------------- |
| GET    | `/stats/summary`    | Get totals by transaction type                  |
| GET    | `/stats/full`       | Get comprehensive stats with category breakdown |
| GET    | `/stats/category`   | Get spending by category                        |
| GET    | `/stats/wallet`     | Get spending by wallet                          |
| GET    | `/stats/trends`     | Get monthly income/expense trends               |
| GET    | `/stats/range`      | Get summary for date range                      |
| GET    | `/stats/comparison` | Compare current vs previous period              |

### Sync (`/sync`)

| Method | Endpoint       | Description                                          |
| ------ | -------------- | ---------------------------------------------------- |
| POST   | `/sync/`       | Sync all data (returns changes since last_synced_at) |
| GET    | `/sync/status` | Get sync status                                      |
| GET    | `/sync/counts` | Get counts of entities to sync                       |

## Data Models

### Transaction

```json
{
	"id": "uuid",
	"user_id": "uuid",
	"wallet_id": "uuid (optional)",
	"group_id": "uuid (optional)",
	"category_id": "uuid (optional)",
	"amount": 100.0,
	"currency": "INR",
	"type": "expense|income|transfer",
	"date": "2024-01-15T10:30:00",
	"note": "Lunch",
	"split_strategy": "equal"
}
```

### Category

```json
{
	"id": "uuid",
	"name": "Food & Dining",
	"icon": "utensils",
	"color": "#FF6B6B",
	"type": "expense|income|both",
	"is_default": true
}
```

### Group Balance

```json
{
	"group_id": "uuid",
	"group_name": "Trip to Goa",
	"total_expenses": 50000,
	"balances": [
		{ "user_id": "uuid", "user_name": "John", "balance": 2500 },
		{ "user_id": "uuid", "user_name": "Jane", "balance": -2500 }
	]
}
```

## Architecture

```
server/
├── app/
│   ├── __init__.py       # FastAPI app setup
│   ├── core/
│   │   ├── config.py     # Settings
│   │   ├── database.py   # MongoDB/Beanie init
│   │   ├── deps.py       # Dependencies (auth)
│   │   ├── logger.py     # Logging
│   │   └── security.py   # JWT/password
│   ├── models/
│   │   ├── db_models.py  # Beanie documents
│   │   └── schemas.py    # Pydantic schemas
│   ├── routes/
│   │   ├── auth.py
│   │   ├── users.py
│   │   ├── expenses.py
│   │   ├── wallets.py
│   │   ├── groups.py
│   │   ├── categories.py
│   │   ├── debts.py
│   │   ├── stats.py
│   │   └── sync.py
│   └── services/
│       ├── auth_service.py
│       ├── user_service.py
│       ├── expense_service.py
│       ├── wallet_service.py
│       ├── group_service.py
│       ├── debt_service.py
│       └── stats_service.py
├── main.py               # Entry point
└── uploads/              # Bill images
```

## Notes

- Uses **Beanie** (ODM) + **Motor** for async MongoDB access
- All services use async operations
- Wallet balances auto-update on transaction create/update/delete
- Groups auto-add creator as admin member
- Uploads are stored locally in `/uploads`
- API docs available at `/docs` (Swagger UI)
