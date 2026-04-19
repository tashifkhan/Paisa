"""HTTP client for the Paisa backend API."""

from __future__ import annotations

import asyncio
import os
from datetime import datetime, timedelta, timezone
from typing import Any

import httpx
import jwt as pyjwt
from dotenv import load_dotenv

load_dotenv()

_API_URL = os.getenv("PAISA_API_URL", "http://localhost:8000").rstrip("/")
_TOKEN: str | None = os.getenv("PAISA_API_TOKEN") or None
_EMAIL = os.getenv("PAISA_EMAIL", "")
_PASSWORD = os.getenv("PAISA_PASSWORD", "")
_STATIC_TOKEN = bool(os.getenv("PAISA_API_TOKEN"))  # True when token was supplied via env

# Refresh the token this many seconds before it actually expires
_REFRESH_MARGIN_SECONDS = 5 * 60  # 5 minutes

# Prevents concurrent login races when multiple tools are called simultaneously
_auth_lock = asyncio.Lock()


# ─── Token helpers ───────────────────────────────────────────────────────────


def _token_expiry() -> datetime | None:
    """Decode the cached token (without verifying signature) and return its expiry."""
    if not _TOKEN:
        return None
    try:
        payload = pyjwt.decode(
            _TOKEN,
            options={"verify_signature": False},
            algorithms=["HS256"],
        )
        exp = payload.get("exp")
        if exp:
            return datetime.fromtimestamp(exp, tz=timezone.utc)
    except Exception:
        pass
    return None


def _token_needs_refresh() -> bool:
    """Return True if the token is missing, expired, or expiring soon."""
    if not _TOKEN:
        return True
    expiry = _token_expiry()
    if expiry is None:
        # Can't decode → treat as valid (maybe a opaque static token)
        return False
    return datetime.now(timezone.utc) >= expiry - timedelta(seconds=_REFRESH_MARGIN_SECONDS)


# ─── Authentication ──────────────────────────────────────────────────────────


async def _authenticate() -> str:
    """Authenticate with email/password and return a fresh JWT."""
    if not _EMAIL or not _PASSWORD:
        raise RuntimeError(
            "Set PAISA_API_TOKEN or both PAISA_EMAIL + PAISA_PASSWORD in your .env"
        )
    async with httpx.AsyncClient(base_url=_API_URL, timeout=15) as c:
        r = await c.post(
            "/auth/login",
            data={"username": _EMAIL, "password": _PASSWORD},
        )
        if r.status_code == 401:
            raise RuntimeError("Invalid Paisa credentials (PAISA_EMAIL / PAISA_PASSWORD)")
        r.raise_for_status()
        return r.json()["access_token"]


async def _get_token() -> str:
    """
    Return a valid JWT, proactively refreshing it if it is close to expiry.

    For static PAISA_API_TOKEN tokens (no credentials available) we skip
    proactive refresh and rely on the 401-retry path instead.
    """
    global _TOKEN

    # Fast path: token is fine
    if _TOKEN and not _token_needs_refresh():
        return _TOKEN

    # Slow path: need to (re-)authenticate
    async with _auth_lock:
        # Re-check inside the lock — another coroutine may have refreshed already
        if _TOKEN and not _token_needs_refresh():
            return _TOKEN

        if _STATIC_TOKEN:
            # Supplied via env; we have no credentials to refresh with.
            # Return as-is and let the 401-retry path handle actual expiry.
            return _TOKEN  # type: ignore[return-value]

        expiry = _token_expiry()
        if expiry:
            remaining = expiry - datetime.now(timezone.utc)
            print(
                f"[paisa-mcp] Token expires in {remaining.seconds // 60}m — refreshing",
                flush=True,
            )
        else:
            print("[paisa-mcp] Obtaining token via credentials", flush=True)

        _TOKEN = await _authenticate()
        return _TOKEN  # type: ignore[return-value]


def _invalidate_token() -> None:
    """Clear the cached token so the next call re-authenticates."""
    global _TOKEN
    if not _STATIC_TOKEN:
        _TOKEN = None


async def _headers() -> dict[str, str]:
    return {"Authorization": f"Bearer {await _get_token()}"}


# ─── Request execution ────────────────────────────────────────────────────────


async def _request(method: str, path: str, **kwargs: Any) -> Any:
    """
    Execute an authenticated HTTP request.

    On a 401 response the token is invalidated and the request retried once.
    This handles the rare case where the server rejects a token that hasn't
    yet hit the proactive refresh threshold (e.g. the server restarted with a
    new secret, or a static env token was revoked).
    """
    headers = await _headers()
    async with httpx.AsyncClient(base_url=_API_URL, timeout=15) as c:
        r = await c.request(method, path, headers=headers, **kwargs)
        if r.status_code == 401:
            _invalidate_token()
            headers = await _headers()  # re-authenticates via _get_token()
            async with httpx.AsyncClient(base_url=_API_URL, timeout=15) as c2:
                r = await c2.request(method, path, headers=headers, **kwargs)
        r.raise_for_status()
        return r.json()


# ─── Public API ───────────────────────────────────────────────────────────────


async def get(path: str, params: dict[str, Any] | None = None) -> Any:
    return await _request("GET", path, params=params)


async def post(path: str, json: dict[str, Any] | None = None, data: dict[str, Any] | None = None) -> Any:
    return await _request("POST", path, json=json, data=data)


async def put(path: str, json: dict[str, Any]) -> Any:
    return await _request("PUT", path, json=json)


async def delete(path: str) -> Any:
    return await _request("DELETE", path)


async def validate_auth() -> str:
    """
    Validate authentication on startup.
    Returns the authenticated user's email, or raises on failure.
    """
    token = await _get_token()
    async with httpx.AsyncClient(base_url=_API_URL, timeout=15) as c:
        r = await c.get("/users/me", headers={"Authorization": f"Bearer {token}"})
        if r.status_code == 401:
            _invalidate_token()
            raise RuntimeError(
                "PAISA_API_TOKEN is set but rejected by the server (expired or invalid). "
                "Provide fresh credentials via PAISA_EMAIL + PAISA_PASSWORD instead."
            )
        r.raise_for_status()
        data = r.json()

    expiry = _token_expiry()
    if expiry:
        remaining = expiry - datetime.now(timezone.utc)
        days = remaining.days
        hours = remaining.seconds // 3600
        print(f"[paisa-mcp] Token valid for ~{days}d {hours}h", flush=True)

    return data.get("email", "unknown")
