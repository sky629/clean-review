from __future__ import annotations

import os
from dataclasses import dataclass
from typing import Mapping


@dataclass(frozen=True, slots=True)
class WorkerSettings:
    kafka_bootstrap_servers: str
    input_topic: str
    output_topic: str
    consumer_group_id: str
    postgres_dsn: str
    gemini_api_key: str
    gemini_model: str
    analyzer_mode: str
    poll_timeout_seconds: float
    naver_search_client_id: str
    naver_search_client_secret: str
    collector_max_reviews_per_source: int
    retry_max_attempts: int
    retry_base_delay_seconds: int
    retry_multiplier: int
    retry_max_delay_seconds: int

    @classmethod
    def from_env(cls, env: Mapping[str, str] | None = None) -> WorkerSettings:
        values = os.environ if env is None else env
        return cls(
            kafka_bootstrap_servers=values.get(
                "KAFKA_BOOTSTRAP_SERVERS", "localhost:9092"
            ),
            input_topic=values.get(
                "KAFKA_TOPIC_REVIEW_COLLECTION_REQUESTED",
                "review.collection.requested",
            ),
            output_topic=values.get(
                "KAFKA_TOPIC_REVIEW_ANALYSIS_COMPLETED",
                "review.analysis.completed",
            ),
            consumer_group_id=values.get(
                "WORKER_CONSUMER_GROUP_ID", "review-analysis-worker"
            ),
            postgres_dsn=_postgres_dsn(values),
            gemini_api_key=values.get("GEMINI_API_KEY", ""),
            gemini_model=values.get("GEMINI_MODEL", "gemini-2.5-flash"),
            analyzer_mode=values.get("REVIEW_ANALYZER_MODE", "crewai"),
            poll_timeout_seconds=float(
                values.get("WORKER_POLL_TIMEOUT_SECONDS", "1.0")
            ),
            naver_search_client_id=values.get("NAVER_SEARCH_CLIENT_ID", ""),
            naver_search_client_secret=values.get("NAVER_SEARCH_CLIENT_SECRET", ""),
            collector_max_reviews_per_source=int(
                values.get("REVIEW_COLLECTOR_MAX_REVIEWS_PER_SOURCE", "100")
            ),
            retry_max_attempts=int(values.get("WORKER_RETRY_MAX_ATTEMPTS", "3")),
            retry_base_delay_seconds=int(
                values.get("WORKER_RETRY_BASE_DELAY_SECONDS", "30")
            ),
            retry_multiplier=int(values.get("WORKER_RETRY_MULTIPLIER", "2")),
            retry_max_delay_seconds=int(
                values.get("WORKER_RETRY_MAX_DELAY_SECONDS", "900")
            ),
        )


def _postgres_dsn(values: Mapping[str, str]) -> str:
    explicit = values.get("POSTGRES_DSN")
    if explicit:
        return explicit

    user = values.get("POSTGRES_USER", "clean_review")
    password = values.get("POSTGRES_PASSWORD", "clean_review")
    host = values.get("POSTGRES_HOST", "localhost")
    port = values.get("POSTGRES_PORT", "5432")
    database = values.get("POSTGRES_DB", "clean_review")
    return f"postgresql://{user}:{password}@{host}:{port}/{database}"
