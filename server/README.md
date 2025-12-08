# Paisa - FastAPI Backend

## Quick start

### 1. Install dependencies

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
uv run main.py
# OR
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

## API Endpoints

- **Auth**

  - `POST /auth/request-otp` — request signup OTP (email logged to console)
  - `POST /auth/verify-otp` — verify OTP and create user
  - `POST /auth/login` — login with email & password

- **Users**

  - `PUT /users/push-token` — update device push token
  - `GET /users/me` — get current user details

- **Expenses**

  - `POST /expenses/upload-bill` — upload a bill image
  - `POST /expenses/add` — add a transaction
  - `GET /expenses/` — list transactions
  - `GET /expenses/export` — export transactions as CSV

- **Wallets & Groups**
  - `GET /wallets/` — list user wallets
  - `GET /groups/` — list groups

## Notes

- Uses **Beanie** (ODM) + **Motor** for async MongoDB access.
- Uploads are stored locally in `/uploads`.
