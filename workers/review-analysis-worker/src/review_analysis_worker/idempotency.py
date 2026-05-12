from __future__ import annotations

from collections.abc import Callable
from dataclasses import dataclass
from enum import Enum
from typing import Any, Protocol

from review_analysis_worker.events import EventEnvelope


class IdempotencyStore(Protocol):
    def claim(self, key: str) -> bool:
        """Return true when this worker may process the key."""

    def mark_succeeded(self, key: str) -> None:
        """Persist that processing completed."""

    def mark_failed(self, key: str) -> None:
        """Release or mark a failed claim so it can be retried."""


class ProcessingStatus(Enum):
    PROCESSED = "processed"
    DUPLICATE = "duplicate"
    FAILED = "failed"


@dataclass(frozen=True, slots=True)
class DuplicateEvent:
    key: str
    event_id: str


@dataclass(frozen=True, slots=True)
class ProcessingResult:
    status: ProcessingStatus
    detail: Any = None


class InMemoryIdempotencyStore:
    def __init__(self) -> None:
        self._claims: set[str] = set()

    def claim(self, key: str) -> bool:
        if key in self._claims:
            return False
        self._claims.add(key)
        return True

    def mark_succeeded(self, key: str) -> None:
        self._claims.add(key)

    def mark_failed(self, key: str) -> None:
        self._claims.discard(key)


def default_idempotency_key(envelope: EventEnvelope) -> str:
    if envelope.idempotency_key:
        return envelope.idempotency_key
    return f"{envelope.event_type}:{envelope.aggregate_id}:{envelope.event_id}"


class IdempotencyProcessor:
    def __init__(
        self,
        store: IdempotencyStore,
        key_factory: Callable[[EventEnvelope], str] = default_idempotency_key,
    ) -> None:
        self._store = store
        self._key_factory = key_factory

    def process(
        self,
        envelope: EventEnvelope,
        handler: Callable[[EventEnvelope], Any],
    ) -> ProcessingResult:
        key = self._key_factory(envelope)
        if not self._store.claim(key):
            return ProcessingResult(
                status=ProcessingStatus.DUPLICATE,
                detail=DuplicateEvent(key=key, event_id=envelope.event_id),
            )

        try:
            detail = handler(envelope)
        except (
            Exception
        ) as exc:  # noqa: BLE001 - failures are represented for retry routing.
            self._store.mark_failed(key)
            return ProcessingResult(status=ProcessingStatus.FAILED, detail=exc)

        self._store.mark_succeeded(key)
        return ProcessingResult(status=ProcessingStatus.PROCESSED, detail=detail)
