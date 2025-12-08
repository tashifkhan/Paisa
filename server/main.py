"""
Paisa Server Entry Point

Run with: python main.py
Or: uvicorn app:app --reload
"""

import uvicorn


def main():
    """Start the Paisa API server."""
    uvicorn.run(
        "app:app",
        host="0.0.0.0",
        port=8000,
        reload=True,
    )


if __name__ == "__main__":
    main()
