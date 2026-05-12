from __future__ import annotations

from review_analysis_worker.analyzers.crewai import (
    CrewAIReviewAnalyzer,
    CrewAIReviewCrewRunner,
)
from review_analysis_worker.analyzers.gemini import GeminiAnalyzer
from review_analysis_worker.collection_pipeline import CollectionPipeline
from review_analysis_worker.collectors.naver_playwright import (
    NaverBlogPlaywrightCollector,
)
from review_analysis_worker.collectors.playwright import PlaywrightReviewCollector
from review_analysis_worker.collectors.search_discovery import (
    NaverBlogApiSearchDiscovery,
    NaverBlogSearchApiClient,
)
from review_analysis_worker.postgres_storage import PostgresReviewStorage
from review_analysis_worker.runtime.config import WorkerSettings
from review_analysis_worker.runtime.first_image_ocr import GeminiFirstImageOcr
from review_analysis_worker.runtime.gemini import GoogleGenaiGeminiClient
from review_analysis_worker.runtime.kafka import (
    JsonKafkaConsumer,
    JsonKafkaProducer,
    create_confluent_consumer,
    create_confluent_producer,
)
from review_analysis_worker.runtime.postgres import connect_postgres
from review_analysis_worker.worker import ReviewAnalysisWorker


def build_pipeline(
    *,
    settings: WorkerSettings,
    gemini_client=None,
    review_crew_runner=None,
    postgres_connection=None,
) -> CollectionPipeline:
    connection = postgres_connection or connect_postgres(settings.postgres_dsn)
    if settings.analyzer_mode == "gemini":
        client = gemini_client or GoogleGenaiGeminiClient(settings.gemini_api_key)
        analyzer = GeminiAnalyzer(client=client, model=settings.gemini_model)
        first_image_ocr = GeminiFirstImageOcr(
            client=client, model=settings.gemini_model
        )
    else:
        runner = review_crew_runner or CrewAIReviewCrewRunner(
            model=settings.gemini_model,
            api_key=settings.gemini_api_key,
        )
        analyzer = CrewAIReviewAnalyzer(runner=runner, model=settings.gemini_model)
        first_image_ocr = None

    return CollectionPipeline(
        collector=_build_collector(settings),
        analyzer=analyzer,
        storage=PostgresReviewStorage(connection),
        first_image_ocr=first_image_ocr,
    )


def _build_collector(settings: WorkerSettings):
    naver_api_client = NaverBlogSearchApiClient(
        client_id=settings.naver_search_client_id,
        client_secret=settings.naver_search_client_secret,
    )
    return PlaywrightReviewCollector(
        source_collectors={
            "NAVER_BLOG": NaverBlogPlaywrightCollector(
                discovery=NaverBlogApiSearchDiscovery(
                    client=naver_api_client,
                    max_results=settings.collector_max_reviews_per_source,
                ),
                max_reviews=settings.collector_max_reviews_per_source,
            )
        }
    )


def build_worker(settings: WorkerSettings) -> ReviewAnalysisWorker:
    pipeline = build_pipeline(settings=settings)
    return ReviewAnalysisWorker(
        consumer=JsonKafkaConsumer(
            create_confluent_consumer(
                bootstrap_servers=settings.kafka_bootstrap_servers,
                group_id=settings.consumer_group_id,
            ),
            topic=settings.input_topic,
            poll_timeout_seconds=settings.poll_timeout_seconds,
        ),
        producer=JsonKafkaProducer(
            create_confluent_producer(
                bootstrap_servers=settings.kafka_bootstrap_servers
            ),
        ),
        output_topic=settings.output_topic,
        handler=pipeline.handle,
    )
