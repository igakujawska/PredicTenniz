from contextlib import asynccontextmanager
from fastapi import FastAPI, HTTPException, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

from .engine import PredictionEngine
from .schemas import ApiError, ValidationIssue, BatchResult, ModelDescription, PredictionBatch


@asynccontextmanager
async def lifespan(app):
    app.state.engine = PredictionEngine()
    yield


app = FastAPI(title="PredictTeniz ML API", version="1.0.0", lifespan=lifespan,
              description="Internal prediction engine. Spring Boot owns players, tournaments and PostgreSQL.")


@app.exception_handler(RequestValidationError)
async def invalid_request(request: Request, error: RequestValidationError):
    issues = [ValidationIssue(location=list(item["loc"]), message=item["msg"]) for item in error.errors()]
    return JSONResponse(status_code=422, content=ApiError(detail="Invalid prediction request", errors=issues).model_dump(by_alias=True))


@app.get("/health/live", tags=["health"])
def liveness():
    return {"status": "UP"}


@app.get("/health/ready", tags=["health"])
def readiness(request: Request):
    if not getattr(request.app.state, "engine", None):
        raise HTTPException(status_code=503, detail="Model artifacts are not loaded")
    return {"status": "UP", "models": [model.version for model in request.app.state.engine.descriptions()]}


@app.get("/v1/models", response_model=list[ModelDescription], response_model_exclude_none=True, tags=["models"])
def models(request: Request):
    return request.app.state.engine.descriptions()


@app.post("/v1/predict", response_model=BatchResult, tags=["predictions"], responses={422: {"model": ApiError}},
          description="Predict every possible pair once; Java reuses this matrix throughout a tournament draw.")
def predict(batch: PredictionBatch, request: Request):
    try:
        return request.app.state.engine.predict(batch)
    except ValueError as error:
        raise HTTPException(status_code=422, detail=str(error)) from error
