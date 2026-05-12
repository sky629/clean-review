from review_analysis_worker.runtime.config import WorkerSettings


def test_worker_settings_loads_defaults_for_local_runtime() -> None:
    settings = WorkerSettings.from_env({})

    assert settings.kafka_bootstrap_servers == "localhost:9092"
    assert settings.input_topic == "review.collection.requested"
    assert settings.output_topic == "review.analysis.completed"
    assert settings.consumer_group_id == "review-analysis-worker"
    assert settings.gemini_model == "gemini-2.5-flash"
    assert settings.analyzer_mode == "crewai"
    assert settings.collector_max_reviews_per_source == 100


def test_worker_settings_reads_env_overrides() -> None:
    settings = WorkerSettings.from_env(
        {
            "KAFKA_BOOTSTRAP_SERVERS": "kafka:9092",
            "KAFKA_TOPIC_REVIEW_COLLECTION_REQUESTED": "custom.in",
            "KAFKA_TOPIC_REVIEW_ANALYSIS_COMPLETED": "custom.out",
            "WORKER_CONSUMER_GROUP_ID": "worker-test",
            "POSTGRES_DSN": "postgresql://clean_review:clean_review@postgres:5432/clean_review",
            "GEMINI_API_KEY": "secret",
            "GEMINI_MODEL": "gemini-test",
            "REVIEW_ANALYZER_MODE": "gemini",
            "NAVER_SEARCH_CLIENT_ID": "naver-id",
            "NAVER_SEARCH_CLIENT_SECRET": "naver-secret",
            "REVIEW_COLLECTOR_MAX_REVIEWS_PER_SOURCE": "50",
        }
    )

    assert settings.kafka_bootstrap_servers == "kafka:9092"
    assert settings.input_topic == "custom.in"
    assert settings.output_topic == "custom.out"
    assert settings.consumer_group_id == "worker-test"
    assert (
        settings.postgres_dsn
        == "postgresql://clean_review:clean_review@postgres:5432/clean_review"
    )
    assert settings.gemini_api_key == "secret"
    assert settings.gemini_model == "gemini-test"
    assert settings.analyzer_mode == "gemini"
    assert settings.naver_search_client_id == "naver-id"
    assert settings.naver_search_client_secret == "naver-secret"
    assert settings.collector_max_reviews_per_source == 50


def test_worker_settings_builds_postgres_dsn_from_standard_env_parts() -> None:
    settings = WorkerSettings.from_env(
        {
            "POSTGRES_USER": "clean_review",
            "POSTGRES_PASSWORD": "secret",
            "POSTGRES_HOST": "postgres",
            "POSTGRES_PORT": "5432",
            "POSTGRES_DB": "clean_review",
        }
    )

    assert (
        settings.postgres_dsn
        == "postgresql://clean_review:secret@postgres:5432/clean_review"
    )
