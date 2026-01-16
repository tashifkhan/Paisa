from fastapi import APIRouter, Depends, Query, Response, UploadFile, File, HTTPException
from typing import Literal, Optional
from datetime import datetime

from app.core.deps import get_current_user
from app.models.db_models import User, Transaction
from app.services.data_service import DataService

router = APIRouter()


@router.get("/export")
async def export_data(
    format: Literal["csv", "json"] = "csv",
    start_date: Optional[datetime] = None,
    end_date: Optional[datetime] = None,
    current_user: User = Depends(get_current_user),
):
    # Filter transactions by user and date range
    query = {"user_id": current_user.id, "deleted": False}

    if start_date or end_date:
        date_filter = {}
        if start_date:
            date_filter["$gte"] = start_date
        if end_date:
            date_filter["$lte"] = end_date
        query["date"] = date_filter

    transactions = await Transaction.find(query).to_list()

    if format == "csv":
        csv_content = await DataService.export_transactions_csv(transactions)
        return Response(
            content=csv_content,
            media_type="text/csv",
            headers={
                "Content-Disposition": f"attachment; filename=transactions_export_{datetime.now().strftime('%Y%m%d')}.csv"
            },
        )
    else:
        json_content = await DataService.export_transactions_json(transactions)
        return Response(
            content=json_content,
            media_type="application/json",
            headers={
                "Content-Disposition": f"attachment; filename=transactions_export_{datetime.now().strftime('%Y%m%d')}.json"
            },
        )


@router.post("/import")
async def import_data(
    file: UploadFile = File(...),
    current_user: User = Depends(get_current_user),
):
    valid_extensions = ["csv", "json"]
    file_ext = file.filename.split(".")[-1].lower() if "." in file.filename else ""

    if file_ext not in valid_extensions:
        raise HTTPException(
            status_code=400, detail="Invalid file format. Supported: csv, json"
        )

    content = await file.read()
    content_str = content.decode("utf-8")

    count = 0
    if file_ext == "csv":
        count = await DataService.import_transactions_csv(content_str, current_user.id)
    elif file_ext == "json":
        count = await DataService.import_transactions_json(content_str, current_user.id)

    return {"message": f"Successfully imported {count} transactions"}
