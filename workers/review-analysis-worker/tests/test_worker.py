import inspect

from review_analysis_worker.collection_pipeline import CollectionPipeline
from review_analysis_worker.collectors.collected_review import CollectedReview
from review_analysis_worker.events import EventEnvelope
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


def test_worker_acks_failed_message_without_crashing() -> None:
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
    def mark_collection_running(self, collection_run_id: str) -> None:
        self.collection_run_id = collection_run_id

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
