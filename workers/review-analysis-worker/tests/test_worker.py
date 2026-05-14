import inspect

import pytest

from review_analysis_worker.collection_pipeline import CollectionPipeline
from review_analysis_worker.collectors.collected_review import CollectedReview
from review_analysis_worker.events import EventEnvelope
from review_analysis_worker.retry import RetryJob, RetryPolicy
from review_analysis_worker.worker import KafkaMessage, ReviewAnalysisWorker


class FakeConsumer:
    def __init__(self, messages: list[KafkaMessage]) -> None:
        self.messages = messages
        self.acked: list[KafkaMessage] = []

    def poll(self) -> KafkaMessage | None:
        if not self.messages:
            return None
        return self.messages.pop(0)

    def ack(self, message: KafkaMessage) -> None:
        self.acked.append(message)


class FakeProducer:
    def __init__(self) -> None:
        self.produced: list[tuple[str, dict]] = []

    def produce(self, topic: str, value: dict) -> None:
        self.produced.append((topic, value))


class FailingProducer:
    def produce(self, topic: str, value: dict) -> None:
        raise RuntimeError("kafka unavailable")


class FakeRetryStore:
    def __init__(self, due_job: RetryJob | None = None) -> None:
        self.due_job = due_job
        self.failures = []
        self.republished = []
        self.republish_failures = []

    def record_failure(self, *, topic, envelope, error, policy):
        self.failures.append((topic, envelope, error, policy))

    def claim_due(self):
        job = self.due_job
        self.due_job = None
        return job

    def mark_republished(self, job):
        self.republished.append(job)

    def mark_republish_failed(self, job, error):
        self.republish_failures.append((job, error))


def test_worker_processes_one_kafka_message_without_fastapi_dependency() -> None:
    envelope = EventEnvelope.from_dict(
        {
            "event_id": "evt-1",
            "event_type": "review.scraped",
            "aggregate_id": "target-1",
            "occurred_at": "2026-05-07T03:15:00Z",
            "payload": {"review_id": "r-1"},
        }
    )
    consumer = FakeConsumer(
        [KafkaMessage(topic="reviews.raw", value=envelope.to_dict())]
    )
    producer = FakeProducer()
    handled: list[str] = []

    worker = ReviewAnalysisWorker(
        consumer=consumer,
        producer=producer,
        output_topic="reviews.analyzed",
        handler=lambda event: {"review_id": handled.append(event.event_id) or "r-1"},
    )

    processed = worker.run_once()

    import review_analysis_worker.worker as worker_module

    assert processed is True
    assert handled == ["evt-1"]
    assert producer.produced == [("reviews.analyzed", {"review_id": "r-1"})]
    assert consumer.acked == [
        KafkaMessage(topic="reviews.raw", value=envelope.to_dict())
    ]
    assert "fastapi" not in inspect.getsource(worker_module).lower()


def test_worker_does_not_ack_failed_message_and_stops_processing() -> None:
    envelope = EventEnvelope.from_dict(
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
                "source": "NAVER_BLOG",
                "keyword": "성수동 파스타 맛집",
            },
        }
    )
    message = KafkaMessage(
        topic="review.collection.requested", value=envelope.to_dict()
    )
    consumer = FakeConsumer([message])
    producer = FakeProducer()

    worker = ReviewAnalysisWorker(
        consumer=consumer,
        producer=producer,
        output_topic="review.analysis.completed",
        handler=lambda event: (_ for _ in ()).throw(RuntimeError("collector failed")),
    )

    with pytest.raises(RuntimeError, match="collector failed"):
        worker.run_once()
    assert consumer.acked == []
    assert producer.produced == []


def test_worker_records_retry_then_acks_failed_message() -> None:
    envelope = EventEnvelope.from_dict(
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
                "source": "NAVER_BLOG",
                "keyword": "성수동 파스타 맛집",
            },
        }
    )
    message = KafkaMessage(
        topic="review.collection.requested", value=envelope.to_dict()
    )
    consumer = FakeConsumer([message])
    producer = FakeProducer()
    retry_store = FakeRetryStore()
    retry_policy = RetryPolicy(max_attempts=3, base_delay_seconds=1)

    worker = ReviewAnalysisWorker(
        consumer=consumer,
        producer=producer,
        output_topic="review.analysis.completed",
        handler=lambda event: (_ for _ in ()).throw(RuntimeError("collector failed")),
        retry_store=retry_store,
        retry_policy=retry_policy,
    )

    assert worker.run_once() is True
    assert consumer.acked == [message]
    assert producer.produced == []
    assert retry_store.failures[0][0] == "review.collection.requested"
    assert retry_store.failures[0][1].event_id == "evt-1"
    assert retry_store.failures[0][3] == retry_policy


def test_worker_republishes_due_retry_when_kafka_is_idle() -> None:
    retry_job = RetryJob(
        id="retry-1",
        topic="review.collection.requested",
        original_event_id="evt-1",
        idempotency_key="review-collection-request:target-1",
        correlation_id="corr-1",
        payload={
            "event_id": "evt-1",
            "event_type": "review.collection.requested.v1",
            "schema_version": 1,
            "occurred_at": "2026-05-07T03:15:00Z",
            "correlation_id": "corr-1",
            "idempotency_key": "review-collection-request:target-1",
            "aggregate_id": "target-1",
            "payload": {"collection_run_id": "run-1"},
        },
        attempt=1,
        max_attempts=3,
    )
    retry_store = FakeRetryStore(due_job=retry_job)
    producer = FakeProducer()
    worker = ReviewAnalysisWorker(
        consumer=FakeConsumer([]),
        producer=producer,
        output_topic="review.analysis.completed",
        handler=lambda event: {"ok": True},
        retry_store=retry_store,
    )

    assert worker.run_once() is True
    assert producer.produced[0][0] == "review.collection.requested"
    retry_envelope = producer.produced[0][1]
    assert retry_envelope["event_id"] != "evt-1"
    assert retry_envelope["idempotency_key"] == "review-collection-request:target-1"
    assert retry_envelope["retry"] == {
        "original_event_id": "evt-1",
        "attempt": 1,
        "max_attempts": 3,
    }
    assert retry_store.republished == [retry_job]


def test_worker_releases_retry_job_when_republish_fails() -> None:
    retry_job = RetryJob(
        id="retry-1",
        topic="review.collection.requested",
        original_event_id="evt-1",
        idempotency_key="review-collection-request:target-1",
        correlation_id="corr-1",
        payload={
            "event_id": "evt-1",
            "event_type": "review.collection.requested.v1",
            "schema_version": 1,
            "occurred_at": "2026-05-07T03:15:00Z",
            "correlation_id": "corr-1",
            "idempotency_key": "review-collection-request:target-1",
            "aggregate_id": "target-1",
            "payload": {"collection_run_id": "run-1"},
        },
        attempt=1,
        max_attempts=3,
    )
    retry_store = FakeRetryStore(due_job=retry_job)
    worker = ReviewAnalysisWorker(
        consumer=FakeConsumer([]),
        producer=FailingProducer(),
        output_topic="review.analysis.completed",
        handler=lambda event: {"ok": True},
        retry_store=retry_store,
    )

    with pytest.raises(RuntimeError, match="kafka unavailable"):
        worker.run_once()

    assert retry_store.republished == []
    assert retry_store.republish_failures[0][0] == retry_job
    assert str(retry_store.republish_failures[0][1]) == "kafka unavailable"


def test_worker_acks_noop_duplicate_without_producing_event() -> None:
    envelope = EventEnvelope.from_dict(
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
                "source": "NAVER_BLOG",
                "keyword": "성수동 파스타 맛집",
            },
        }
    )
    message = KafkaMessage(
        topic="review.collection.requested", value=envelope.to_dict()
    )
    consumer = FakeConsumer([message])
    producer = FakeProducer()

    worker = ReviewAnalysisWorker(
        consumer=consumer,
        producer=producer,
        output_topic="review.analysis.completed",
        handler=lambda event: None,
    )

    assert worker.run_once() is True
    assert consumer.acked == [message]
    assert producer.produced == []


class FakeCollector:
    def collect(self, *, source: str, keyword: str, **kwargs):
        return [
            CollectedReview(
                review_id="naver-101",
                author="summer",
                rating=5,
                created_at="2026-05-06",
                body="떡이 쫄깃했고 포장도 깔끔해서 집에 와서도 먹기 좋았습니다.",
            )
        ]


class FakeAnalyzer:
    model = "gemini-2.5-flash"

    def analyze(self, request):
        return {
            "viral_score": 12.0,
            "quality_score": 91.0,
            "summary": "구체적인 구매 경험이 있는 신뢰도 높은 후기입니다.",
            "pros": ["포장 상태가 구체적으로 언급됨"],
            "cons": [],
            "detected_patterns": [],
            "evidence": [],
        }


class FakeStorage:
    def mark_collection_running(self, collection_run_id: str) -> bool:
        self.collection_run_id = collection_run_id
        return True

    def find_review_by_source_and_url_hash(
        self, *, source: str, canonical_url_hash: str
    ):
        return None

    def analysis_exists(self, **kwargs) -> bool:
        return False

    def save_review(self, review):
        return review

    def save_analysis(self, analysis):
        return analysis

    def save_report(self, report):
        return report

    def mark_collection_completed(self, collection_run_id: str) -> None:
        self.completed_collection_run_id = collection_run_id


def test_worker_handles_collection_requested_event_through_pipeline() -> None:
    envelope = EventEnvelope.from_dict(
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
                "source": "NAVER_BLOG",
                "keyword": "성수동 파스타 맛집",
            },
        }
    )
    consumer = FakeConsumer(
        [KafkaMessage(topic="review.collection.requested", value=envelope.to_dict())]
    )
    producer = FakeProducer()
    pipeline = CollectionPipeline(FakeCollector(), FakeAnalyzer(), FakeStorage())
    worker = ReviewAnalysisWorker(
        consumer=consumer,
        producer=producer,
        output_topic="review.analysis.completed",
        handler=pipeline.handle,
    )

    assert worker.run_once() is True
    assert producer.produced[0][0] == "review.analysis.completed"
    assert producer.produced[0][1]["event_type"] == "review.analysis.completed.v1"
