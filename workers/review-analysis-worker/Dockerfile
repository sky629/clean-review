FROM python:3.11-slim

WORKDIR /app

ENV PYTHONUNBUFFERED=1
ENV PYTHONPATH=/app/src

COPY pyproject.toml uv.lock ./
RUN pip install --no-cache-dir \
    confluent-kafka==2.14.0 \
    "crewai>=1.0.0" \
    google-genai==1.75.0 \
    playwright==1.57.0 \
    "psycopg[binary]==3.3.4"
RUN python -m playwright install --with-deps chromium

COPY src ./src

CMD ["python", "-m", "review_analysis_worker"]
