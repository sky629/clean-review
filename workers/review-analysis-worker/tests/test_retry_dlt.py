from review_analysis_worker.events import EventEnvelope
from review_analysis_worker.retry import DeadLetterEvent, RetryDecision, RetryPolicy


def _envelope() -> EventEnvelope:
    return EventEnvelope.from_dict(
        {
            "event_id": "evt-1",
            "event_type": "review.scraped",
            "aggregate_id": "target-1",
            "occurred_at": "2026-05-07T03:15:00Z",
            "payload": {"review_id": "r-1"},
        }
    )


def test_retry_policy_allows_attempts_until_limit() -> None:
    policy = RetryPolicy(max_attempts=3, base_delay_seconds=2, multiplier=2)

    assert policy.decide(attempt=1, error=RuntimeError("boom")) == RetryDecision(
        should_retry=True,
        delay_seconds=2,
        reason="RuntimeError: boom",
    )
    assert policy.decide(attempt=3, error=RuntimeError("boom")).should_retry is False


def test_dead_letter_event_preserves_original_envelope_and_reason() -> None:
    dlt = DeadLetterEvent.from_failure(
        envelope=_envelope(),
        reason="exhausted retries",
        attempt=3,
    )

    assert dlt.original_event["event_id"] == "evt-1"
    assert dlt.reason == "exhausted retries"
    assert dlt.attempt == 3
