from datetime import UTC, datetime

import pytest

from review_analysis_worker.events import EnvelopeValidationError, EventEnvelope


def test_event_envelope_round_trips_dict_and_iso_timestamp() -> None:
    envelope = EventEnvelope.from_dict(
        {
            "event_id": "evt-1",
            "event_type": "review.scraped",
            "aggregate_id": "target-1",
            "occurred_at": "2026-05-07T03:15:00Z",
            "schema_version": 1,
            "payload": {"review_id": "r-1"},
            "correlation_id": "corr-1",
            "idempotency_key": "idem-1",
            "retry": {"original_event_id": "evt-original", "attempt": 1},
        }
    )

    assert envelope.occurred_at == datetime(2026, 5, 7, 3, 15, tzinfo=UTC)
    assert envelope.retry == {"original_event_id": "evt-original", "attempt": 1}
    assert envelope.to_dict() == {
        "event_id": "evt-1",
        "event_type": "review.scraped",
        "aggregate_id": "target-1",
        "occurred_at": "2026-05-07T03:15:00Z",
        "schema_version": 1,
        "payload": {"review_id": "r-1"},
        "correlation_id": "corr-1",
        "idempotency_key": "idem-1",
        "retry": {"original_event_id": "evt-original", "attempt": 1},
    }


@pytest.mark.parametrize("field", ["event_id", "event_type", "aggregate_id"])
def test_event_envelope_requires_identity_fields(field: str) -> None:
    raw = {
        "event_id": "evt-1",
        "event_type": "review.scraped",
        "aggregate_id": "target-1",
        "occurred_at": "2026-05-07T03:15:00Z",
        "payload": {},
    }
    raw[field] = ""

    with pytest.raises(EnvelopeValidationError, match=field):
        EventEnvelope.from_dict(raw)


def test_event_envelope_requires_object_payload() -> None:
    with pytest.raises(EnvelopeValidationError, match="payload"):
        EventEnvelope.from_dict(
            {
                "event_id": "evt-1",
                "event_type": "review.scraped",
                "aggregate_id": "target-1",
                "occurred_at": "2026-05-07T03:15:00Z",
                "payload": [],
            }
        )
