from __future__ import annotations

from dataclasses import dataclass
from enum import Enum
from typing import Any

from review_analysis_worker.events import EventEnvelope


@dataclass(frozen=True, slots=True)
class RetryDecision:
    should_retry: bool
    delay_seconds: int
    reason: str


@dataclass(frozen=True, slots=True)
class RetryPolicy:
    max_attempts: int = 3
    base_delay_seconds: int = 1
    multiplier: int = 2
    max_delay_seconds: int = 60

    def decide(self, attempt: int, error: Exception) -> RetryDecision:
        reason = f"{type(error).__name__}: {error}"
        if attempt >= self.max_attempts:
            return RetryDecision(
                should_retry=False,
                delay_seconds=0,
                reason=reason,
            )

        delay = self.base_delay_seconds * (self.multiplier ** max(attempt - 1, 0))
        return RetryDecision(
            should_retry=True,
            delay_seconds=min(delay, self.max_delay_seconds),
            reason=reason,
        )


class RetryJobStatus(Enum):
    PENDING = "PENDING"
    REPUBLISHED = "REPUBLISHED"
    DEAD_LETTERED = "DEAD_LETTERED"


@dataclass(frozen=True, slots=True)
class RetryJob:
    id: str
    topic: str
    original_event_id: str
    idempotency_key: str
    correlation_id: str
    payload: dict[str, Any]
    attempt: int
    max_attempts: int


@dataclass(frozen=True, slots=True)
class DeadLetterEvent:
    original_event: dict[str, Any]
    reason: str
    attempt: int

    @classmethod
    def from_failure(
        cls,
        envelope: EventEnvelope,
        reason: str,
        attempt: int,
    ) -> DeadLetterEvent:
        return cls(
            original_event=envelope.to_dict(),
            reason=reason,
            attempt=attempt,
        )
