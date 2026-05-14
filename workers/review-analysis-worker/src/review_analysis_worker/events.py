from __future__ import annotations

from dataclasses import dataclass
from datetime import UTC, datetime
from typing import Any


class EnvelopeValidationError(ValueError):
    """Raised when an event envelope does not match the worker contract."""


@dataclass(frozen=True, slots=True)
class EventEnvelope:
    event_id: str
    event_type: str
    aggregate_id: str
    occurred_at: datetime
    payload: dict[str, Any]
    schema_version: int = 1
    correlation_id: str | None = None
    idempotency_key: str | None = None
    retry: dict[str, Any] | None = None

    @classmethod
    def from_dict(cls, raw: dict[str, Any]) -> EventEnvelope:
        for field in ("event_id", "event_type", "aggregate_id"):
            value = raw.get(field)
            if not isinstance(value, str) or not value.strip():
                raise EnvelopeValidationError(f"{field} is required")

        payload = raw.get("payload")
        if not isinstance(payload, dict):
            raise EnvelopeValidationError("payload must be an object")

        return cls(
            event_id=raw["event_id"],
            event_type=raw["event_type"],
            aggregate_id=raw["aggregate_id"],
            occurred_at=_parse_datetime(raw.get("occurred_at")),
            schema_version=_parse_schema_version(raw.get("schema_version", 1)),
            payload=payload,
            correlation_id=_optional_string(raw.get("correlation_id")),
            idempotency_key=_optional_string(raw.get("idempotency_key")),
            retry=_optional_object(raw.get("retry")),
        )

    def to_dict(self) -> dict[str, Any]:
        result: dict[str, Any] = {
            "event_id": self.event_id,
            "event_type": self.event_type,
            "aggregate_id": self.aggregate_id,
            "occurred_at": _format_datetime(self.occurred_at),
            "schema_version": self.schema_version,
            "payload": self.payload,
        }
        if self.correlation_id is not None:
            result["correlation_id"] = self.correlation_id
        if self.idempotency_key is not None:
            result["idempotency_key"] = self.idempotency_key
        if self.retry is not None:
            result["retry"] = self.retry
        return result


def _optional_object(value: Any) -> dict[str, Any] | None:
    if value is None:
        return None
    if not isinstance(value, dict):
        raise EnvelopeValidationError("optional envelope object fields must be objects")
    return value


def _optional_string(value: Any) -> str | None:
    if value is None:
        return None
    if not isinstance(value, str):
        raise EnvelopeValidationError("optional envelope fields must be strings")
    return value


def _parse_schema_version(value: Any) -> int:
    if not isinstance(value, int) or value < 1:
        raise EnvelopeValidationError("schema_version must be a positive integer")
    return value


def _parse_datetime(value: Any) -> datetime:
    if isinstance(value, datetime):
        return value.astimezone(UTC)
    if not isinstance(value, str) or not value.strip():
        raise EnvelopeValidationError("occurred_at is required")
    normalized = value.replace("Z", "+00:00")
    try:
        parsed = datetime.fromisoformat(normalized)
    except ValueError as exc:
        raise EnvelopeValidationError("occurred_at must be ISO-8601") from exc
    if parsed.tzinfo is None:
        raise EnvelopeValidationError("occurred_at must include a timezone")
    return parsed.astimezone(UTC)


def _format_datetime(value: datetime) -> str:
    normalized = value.astimezone(UTC)
    return normalized.isoformat().replace("+00:00", "Z")
