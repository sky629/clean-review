from review_analysis_worker.events import EventEnvelope
from review_analysis_worker.postgres_retry import (
    PostgresRetryStore,
    build_retry_envelope,
)
from review_analysis_worker.retry import RetryJob, RetryJobStatus, RetryPolicy


class FakeConnection:
    def __init__(self, fetch_results=None) -> None:
        self.calls: list[tuple[str, tuple]] = []
        self.fetch_results = list(fetch_results or [])

    def execute(self, sql: str, params: tuple = ()):
        self.calls.append((" ".join(sql.split()), params))
        return self

    def fetchone(self):
        if self.fetch_results:
            return self.fetch_results.pop(0)
        return None


class MissingRetryTableConnection:
    def __init__(self) -> None:
        self.rolled_back = False

    def execute(self, sql: str, params: tuple = ()):
        from psycopg.errors import UndefinedTable

        raise UndefinedTable("relation retry_jobs does not exist")

    def rollback(self) -> None:
        self.rolled_back = True


def _envelope() -> EventEnvelope:
    return EventEnvelope.from_dict(
        {
            "event_id": "evt-1",
            "event_type": "review.collection.requested.v1",
            "schema_version": 1,
            "occurred_at": "2026-05-07T03:15:00Z",
            "correlation_id": "corr-1",
            "idempotency_key": "review-collection-request:target-1",
            "aggregate_id": "target-1",
            "payload": {
                "collection_run_id": "run-1",
                "target_id": "target-1",
            },
        }
    )


def test_postgres_retry_store_records_pending_retry_after_failure() -> None:
    connection = FakeConnection(fetch_results=[(0,)])
    store = PostgresRetryStore(connection, consumer_name="review-analysis-worker")

    store.record_failure(
        topic="review.collection.requested",
        envelope=_envelope(),
        error=RuntimeError("gemini timeout"),
        policy=RetryPolicy(max_attempts=3, base_delay_seconds=30),
    )

    executed = "\n".join(call[0] for call in connection.calls)
    assert "select coalesce(max(attempt), 0)" in executed
    assert "insert into retry_jobs" in executed
    assert "on conflict (original_event_id, consumer_name)" in executed
    assert connection.calls[1][1][3] == "evt-1"
    assert connection.calls[1][1][5] == "review-analysis-worker"
    assert connection.calls[1][1][8] == 1
    assert connection.calls[1][1][13] == RetryJobStatus.PENDING.value


def test_postgres_retry_store_keeps_original_event_id_for_retried_event_failure() -> (
    None
):
    raw = _envelope().to_dict()
    raw["event_id"] = "evt-retry-1"
    raw["retry"] = {"original_event_id": "evt-1", "attempt": 1}
    connection = FakeConnection(fetch_results=[(1,)])
    store = PostgresRetryStore(connection, consumer_name="review-analysis-worker")

    store.record_failure(
        topic="review.collection.requested",
        envelope=EventEnvelope.from_dict(raw),
        error=RuntimeError("gemini timeout"),
        policy=RetryPolicy(max_attempts=3, base_delay_seconds=30),
    )

    assert connection.calls[0][1] == ("evt-1", "review-analysis-worker")
    assert connection.calls[1][1][3] == "evt-1"
    assert connection.calls[1][1][8] == 2


def test_postgres_retry_store_moves_exhausted_failure_to_dead_letter() -> None:
    connection = FakeConnection(fetch_results=[(2,)])
    store = PostgresRetryStore(connection, consumer_name="review-analysis-worker")

    store.record_failure(
        topic="review.collection.requested",
        envelope=_envelope(),
        error=RuntimeError("gemini timeout"),
        policy=RetryPolicy(max_attempts=3),
    )

    executed = "\n".join(call[0] for call in connection.calls)
    assert "insert into dead_letter_events" in executed
    assert "update retry_jobs set status = %s" in executed
    assert connection.calls[1][1][2] == "evt-1"
    assert connection.calls[1][1][4] == "review-analysis-worker"
    assert connection.calls[2][1][0] == RetryJobStatus.DEAD_LETTERED.value


def test_postgres_retry_store_claims_due_retry_job() -> None:
    payload = _envelope().to_dict()
    connection = FakeConnection(
        fetch_results=[
            (
                "retry-1",
                "review.collection.requested",
                "evt-1",
                "review-collection-request:target-1",
                "corr-1",
                payload,
                1,
                3,
            )
        ]
    )
    store = PostgresRetryStore(connection, consumer_name="review-analysis-worker")

    job = store.claim_due()

    assert job == RetryJob(
        id="retry-1",
        topic="review.collection.requested",
        original_event_id="evt-1",
        idempotency_key="review-collection-request:target-1",
        correlation_id="corr-1",
        payload=payload,
        attempt=1,
        max_attempts=3,
    )
    assert "for update skip locked" in connection.calls[0][0]


def test_postgres_retry_store_skips_due_claim_until_schema_exists() -> None:
    connection = MissingRetryTableConnection()
    store = PostgresRetryStore(connection, consumer_name="review-analysis-worker")

    assert store.claim_due() is None
    assert connection.rolled_back is True


def test_postgres_retry_store_releases_job_when_republish_fails() -> None:
    job = RetryJob(
        id="retry-1",
        topic="review.collection.requested",
        original_event_id="evt-1",
        idempotency_key="review-collection-request:target-1",
        correlation_id="corr-1",
        payload=_envelope().to_dict(),
        attempt=1,
        max_attempts=3,
    )
    connection = FakeConnection()
    store = PostgresRetryStore(connection, consumer_name="review-analysis-worker")

    store.mark_republish_failed(job, RuntimeError("kafka unavailable"))

    assert "update retry_jobs set status = %s" in connection.calls[0][0]
    assert "next_attempt_at = now() + interval '30 seconds'" in connection.calls[0][0]
    assert connection.calls[0][1] == (
        RetryJobStatus.PENDING.value,
        "RuntimeError",
        "kafka unavailable",
        "retry-1",
    )


def test_build_retry_envelope_keeps_idempotency_key_and_adds_retry_metadata() -> None:
    job = RetryJob(
        id="retry-1",
        topic="review.collection.requested",
        original_event_id="evt-1",
        idempotency_key="review-collection-request:target-1",
        correlation_id="corr-1",
        payload=_envelope().to_dict(),
        attempt=2,
        max_attempts=3,
    )

    retry_envelope = build_retry_envelope(job)

    assert retry_envelope["event_id"] != "evt-1"
    assert retry_envelope["idempotency_key"] == "review-collection-request:target-1"
    assert retry_envelope["correlation_id"] == "corr-1"
    assert retry_envelope["retry"] == {
        "original_event_id": "evt-1",
        "attempt": 2,
        "max_attempts": 3,
    }
