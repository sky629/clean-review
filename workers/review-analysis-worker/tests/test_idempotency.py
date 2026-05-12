from review_analysis_worker.events import EventEnvelope
from review_analysis_worker.idempotency import (
    DuplicateEvent,
    InMemoryIdempotencyStore,
    IdempotencyProcessor,
    ProcessingStatus,
    default_idempotency_key,
)


def _envelope(event_id: str = "evt-1", review_id: str = "r-1") -> EventEnvelope:
    return EventEnvelope.from_dict(
        {
            "event_id": event_id,
            "event_type": "review.scraped",
            "aggregate_id": "target-1",
            "occurred_at": "2026-05-07T03:15:00Z",
            "payload": {"review_id": review_id},
        }
    )


def test_default_idempotency_key_prefers_explicit_key() -> None:
    envelope = EventEnvelope.from_dict(
        {
            "event_id": "evt-1",
            "event_type": "review.scraped",
            "aggregate_id": "target-1",
            "occurred_at": "2026-05-07T03:15:00Z",
            "payload": {"review_id": "r-1"},
            "idempotency_key": "custom-key",
        }
    )

    assert default_idempotency_key(envelope) == "custom-key"


def test_default_idempotency_key_is_stable_without_explicit_key() -> None:
    assert default_idempotency_key(_envelope()) == "review.scraped:target-1:evt-1"


def test_idempotency_processor_marks_duplicate_without_reprocessing() -> None:
    store = InMemoryIdempotencyStore()
    processor = IdempotencyProcessor(store)
    calls: list[str] = []

    first = processor.process(_envelope(), lambda event: calls.append(event.event_id))
    second = processor.process(_envelope(), lambda event: calls.append(event.event_id))

    assert first.status is ProcessingStatus.PROCESSED
    assert second.status is ProcessingStatus.DUPLICATE
    assert isinstance(second.detail, DuplicateEvent)
    assert calls == ["evt-1"]


def test_idempotency_processor_releases_key_after_failure() -> None:
    store = InMemoryIdempotencyStore()
    processor = IdempotencyProcessor(store)

    def fail_once(_: EventEnvelope) -> None:
        raise RuntimeError("transient")

    failed = processor.process(_envelope(), fail_once)
    retried = processor.process(_envelope(), lambda _: "ok")

    assert failed.status is ProcessingStatus.FAILED
    assert retried.status is ProcessingStatus.PROCESSED
