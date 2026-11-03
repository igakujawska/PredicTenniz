FROM python:3.12-slim
WORKDIR /app
COPY ml/requirements-api.txt /app/requirements-api.txt
RUN pip install --no-cache-dir -r requirements-api.txt
COPY ml/__init__.py /app/ml/__init__.py
COPY ml/service /app/ml/service
COPY ml/artifacts /app/ml/artifacts
EXPOSE 8000
HEALTHCHECK --interval=10s --timeout=3s --start-period=15s --retries=5 CMD python -c "from urllib.request import urlopen; urlopen('http://127.0.0.1:8000/health/ready', timeout=2)"
CMD ["python", "-m", "uvicorn", "ml.service.app:app", "--host", "0.0.0.0", "--port", "8000"]
