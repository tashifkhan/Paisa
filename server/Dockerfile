# Paisa FastAPI Backend Dockerfile
# Multi-stage build for smaller image size

FROM python:3.12-slim as builder

WORKDIR /app

# Install uv for faster dependency installation
RUN pip install uv

# Copy dependency files
COPY pyproject.toml ./
COPY requirements.txt ./

# Install dependencies
RUN uv pip install --system -r requirements.txt

# --- Production stage ---
FROM python:3.12-slim

WORKDIR /app

# Copy installed packages from builder
COPY --from=builder /usr/local/lib/python3.12/site-packages /usr/local/lib/python3.12/site-packages
COPY --from=builder /usr/local/bin /usr/local/bin

# Copy application code
COPY app/ ./app/
COPY main.py ./

# Create uploads directory
RUN mkdir -p /app/uploads

# Set environment variables
ENV PYTHONUNBUFFERED=1
ENV PORT=8000

# Expose port
EXPOSE 8000

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:8000/health')" || exit 1

# Run the application
CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "8000"]
