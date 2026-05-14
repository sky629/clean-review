from __future__ import annotations

import json
import uuid
from datetime import UTC, datetime, timedelta

from psycopg.errors import UndefinedTable

from review_analysis_worker.events import EventEnvelope
from review_analysis_worker.retry import RetryJob, RetryJobStatus, RetryPolicy


class PostgresRetryStore:
    def __init__(self, connection, *, consumer_name: str) -> None:
        self._connection = connection
        self._consumer_name = consumer_name

    def record_failure(
        self,
        *,
        topic: str,
        envelope: EventEnvelope,
        error: Exception,
        policy: RetryPolicy,
    ) -> None:
        attempt = self._next_attempt(envelope)
        decision = policy.decide(attempt, error)
        if not decision.should_retry:
            self._store_dead_letter(
                topic=topic,
                envelope=envelope,
                error=error,
                attempt=attempt,
            )
            self._mark_retry_dead_lettered(envelope)
            return

        now = datetime.now(UTC)
        next_attempt_at = now + timedelta(seconds=decision.delay_seconds)
        self._connection.execute(
            """
            insert into retry_jobs (
                id,
                topic,
                event_type,
                original_event_id,
                idempotency_key,
                consumer_name,
                correlation_id,
                payload,
                attempt,
                max_attempts,
                next_attempt_at,
                last_error_code,
                last_error_message,
                status,
                created_at,
                updated_at
            )
            values (
                %s, %s, %s, %s, %s,
                %s, %s, %s::jsonb, %s, %s,
                %s, %s, %s, %s, %s, %s
            )
            on conflict (original_event_id, consumer_name)
            do update set
                attempt = excluded.attempt,
                max_attempts = excluded.max_attempts,
                next_attempt_at = excluded.next_attempt_at,
                last_error_code = excluded.last_error_code,
                last_error_message = excluded.last_error_message,
                status = excluded.status,
                updated_at = excluded.updated_at
            """,
            (
                str(uuid.uuid4()),
                topic,
                envelope.event_type,
                _original_event_id(envelope),
                _idempotency_key(envelope),
                self._consumer_name,
                envelope.correlation_id or envelope.event_id,
                json.dumps(envelope.to_dict(), ensure_ascii=False),
                attempt,
                policy.max_attempts,
                next_attempt_at,
                type(error).__name__,
                str(error),
                RetryJobStatus.PENDING.value,
                now,
                now,
            ),
        )

    def claim_due(self) -> RetryJob | None:
        try:
            cursor = self._connection.execute(
                """
                update retry_jobs
                   set status = %s,
                       updated_at = now()
                 where id = (
                    select id
                      from retry_jobs
                     where status = %s
                       and next_attempt_at <= now()
                     order by next_attempt_at asc
                     limit 1
                     for update skip locked
                 )
                returning id,
                          topic,
                          original_event_id,
                          idempotency_key,
                          correlation_id,
                          payload,
                          attempt,
                          max_attempts
                """,
                (RetryJobStatus.REPUBLISHED.value, RetryJobStatus.PENDING.value),
            )
        except UndefinedTable:
            self._connection.rollback()
            return None
        row = cursor.fetchone()
        if row is None:
            return None
        return RetryJob(
            id=str(row[0]),
            topic=str(row[1]),
            original_event_id=str(row[2]),
            idempotency_key=str(row[3]),
            correlation_id=str(row[4]),
            payload=_json_payload(row[5]),
            attempt=int(row[6]),
            max_attempts=int(row[7]),
        )

    def mark_republished(self, job: RetryJob) -> None:
        self._connection.execute(
            """
            update retry_jobs
               set status = %s,
                   updated_at = now()
             where id = %s
            """,
            (RetryJobStatus.REPUBLISHED.value, job.id),
        )

    def mark_republish_failed(self, job: RetryJob, error: Exception) -> None:
        self._connection.execute(
            """
            update retry_jobs
               set status = %s,
                   last_error_code = %s,
                   last_error_message = %s,
                   next_attempt_at = now() + interval '30 seconds',
                   updated_at = now()
             where id = %s
            """,
            (
                RetryJobStatus.PENDING.value,
                type(error).__name__,
                str(error),
                job.id,
            ),
        )

    def _next_attempt(self, envelope: EventEnvelope) -> int:
        cursor = self._connection.execute(
            """
            select coalesce(max(attempt), 0)
              from retry_jobs
             where original_event_id = %s
               and consumer_name = %s
            """,
            (_original_event_id(envelope), self._consumer_name),
        )
        row = cursor.fetchone()
        return int(row[0] or 0) + 1

    def _store_dead_letter(
        self,
        *,
        topic: str,
        envelope: EventEnvelope,
        error: Exception,
        attempt: int,
    ) -> None:
        self._connection.execute(
            """
            insert into dead_letter_events (
                id,
                source_topic,
                event_id,
                event_type,
                consumer_name,
                payload,
                error_code,
                error_message,
                failed_at
            )
            values (%s, %s, %s, %s, %s, %s::jsonb, %s, %s, now())
            on conflict (event_id, consumer_name) do nothing
            """,
            (
                str(uuid.uuid4()),
                topic,
                _original_event_id(envelope),
                envelope.event_type,
                self._consumer_name,
                json.dumps(
                    {
                        "attempt": attempt,
                        "event": envelope.to_dict(),
                    },
                    ensure_ascii=False,
                ),
                type(error).__name__,
                str(error),
            ),
        )

    def _mark_retry_dead_lettered(self, envelope: EventEnvelope) -> None:
        self._connection.execute(
            """
            update retry_jobs
               set status = %s,
                   updated_at = now()
             where original_event_id = %s
               and consumer_name = %s
            """,
            (
                RetryJobStatus.DEAD_LETTERED.value,
                _original_event_id(envelope),
                self._consumer_name,
            ),
        )


def build_retry_envelope(job: RetryJob) -> dict[str, object]:
    envelope = dict(job.payload)
    envelope["event_id"] = str(uuid.uuid4())
    envelope["occurred_at"] = datetime.now(UTC).isoformat().replace("+00:00", "Z")
    envelope["correlation_id"] = job.correlation_id
    envelope["idempotency_key"] = job.idempotency_key
    envelope["retry"] = {
        "original_event_id": job.original_event_id,
        "attempt": job.attempt,
        "max_attempts": job.max_attempts,
    }
    return envelope


def _original_event_id(envelope: EventEnvelope) -> str:
    if envelope.retry is not None:
        original_event_id = envelope.retry.get("original_event_id")
        if isinstance(original_event_id, str) and original_event_id.strip():
            return original_event_id
    return envelope.event_id


def _idempotency_key(envelope: EventEnvelope) -> str:
    return envelope.idempotency_key or f"{envelope.event_type}:{envelope.aggregate_id}"


def _json_payload(value) -> dict[str, object]:
    if isinstance(value, dict):
        return value
    if isinstance(value, str):
        loaded = json.loads(value)
        if isinstance(loaded, dict):
            return loaded
    raise TypeError("retry job payload must be a JSON object")
