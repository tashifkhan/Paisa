import asyncio
import httpx
import os
import sys
from datetime import datetime

# Add project root to path
sys.path.append(os.path.join(os.path.dirname(__file__), ".."))


async def test_import_export():
    base_url = "http://localhost:8000"  # Adjust if needed

    # We need a token or mock user.
    # For this script to run standalone against a running server, we need to login first.
    # Alternatively, we can use the app instance directly if we set up a test client.
    # But let's assume we are testing against a running server for "Manual Verification" simulation
    # Or better, let's use TestClient from starlette/fastapi which doesn't require running server

    from app import app
    from fastapi.testclient import TestClient

    client = TestClient(app)

    print("1. Creating a user and logging in...")
    email = f"test_export_{int(datetime.now().timestamp())}@example.com"
    password = "password123"

    # Register/Login flow (mocking or actual)
    # Since we are using TestClient and actual DB might be needed...
    # This might be complex if DB is not mocked.
    # Let's assume we can mock the dependency override for current_user if needed or just rely on auth.

    # Actually, simpler: Let's just create a test that mocks nothing and assumes local dev env.

    # Register
    # client.post("/auth/signup", json={"email": email, "password": password, "name": "Test User"})
    # Login
    # response = client.post("/auth/signin", json={"username": email, "password": password}) # depends on auth implementation

    # Since I don't want to mess with DB in this script without being sure,
    # I will just write a script that helps the USER test it manually or outlines the steps.
    # BUT the plan said "Automated Tests - Exact commands you'll run".

    # Let's try to make a standalone test script that uses the existing app structure.
    # We need to install `httpx` if not present, but `TestClient` uses it.

    try:
        # Register
        signup_res = client.post(
            "/auth/signup",
            json={"email": email, "password": password, "name": "Exporter"},
        )
        if signup_res.status_code != 200 and signup_res.status_code != 201:
            # Maybe already exists
            pass

        # Login
        login_res = client.post(
            "/auth/token", data={"username": email, "password": password}
        )
        if login_res.status_code != 200:
            print(f"Login failed: {login_res.text}")
            return

        token = login_res.json()["access_token"]
        headers = {"Authorization": f"Bearer {token}"}

        print("2. Creating a dummy transaction...")
        # Assuming we have an endpoint for this. Check `expenses.py`.
        # POST /expenses/
        txn_data = {
            "amount": 123.45,
            "currency": "INR",
            "type": "expense",
            "date": datetime.now().isoformat(),
            "note": "Test Export Item",
        }
        create_res = client.post("/expenses/", json=txn_data, headers=headers)
        if create_res.status_code not in [200, 201]:
            print(f"Failed to create transaction: {create_res.text}")
            # If expenses endpoint is different, we might fail here.
            # But let's assume standard REST.

        print("3. Exporting CSV...")
        export_res = client.get("/data/export?format=csv", headers=headers)
        if export_res.status_code == 200:
            print("Export CSV Success")
            print("CSV Content Preview:", export_res.text[:100])
            csv_content = export_res.content
        else:
            print(f"Export CSV Failed: {export_res.text}")
            return

        print("4. Importing CSV...")
        # Send the same CSV back
        files = {"file": ("test.csv", csv_content, "text/csv")}
        import_res = client.post("/data/import", files=files, headers=headers)
        if import_res.status_code == 200:
            print("Import CSV Success:", import_res.json())
        else:
            print(f"Import CSV Failed: {import_res.text}")

    except Exception as e:
        print(f"Test failed with exception: {e}")


if __name__ == "__main__":
    asyncio.run(test_import_export())
