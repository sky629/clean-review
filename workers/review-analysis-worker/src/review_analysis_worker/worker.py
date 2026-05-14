from __future__ import annotations

import logging
from collections.abc import Callable
from dataclasses import dataclass
from typing import Any, Protocol

from review_analysis_worker.events import EventEnvelope
from review_analysis_worker.postgres_retry import build_retry_envelope
from review_analysis_worker.retry import RetryJob, RetryPolicy

logger = logging.getLogger(__name__)


@dataclass(frozen=True, slots=True)
class KafkaMessage:
    topic: str
    value: dict[str, Any]
    key: str | None = None


class KafkaConsumer(Protocol):
    def poll(self) -> KafkaMessage | None:
        """Return the next Kafka message or None when no message is available."""

    def ack(self, message: KafkaMessage) -> None:
        """Acknowledge a processed Kafka message."""


class KafkaProducer(Protocol):
    def produce(self, topic: str, value: dict[str, Any]) -> None:
        """Produce a Kafka message value to a topic."""


class RetryStore(Protocol):
    def record_failure(
        self,
        *,
        topic: str,
        envelope: EventEnvelope,
        error: Exception,
        policy: RetryPolicy,
    ) -> None:
        """Persist retry or DLT state for a failed message."""

    def claim_due(self) -> RetryJob | None:
        """Claim a due retry job for republishing."""

    def mark_republished(self, job: RetryJob) -> None:
        """Mark a retry job as republished."""

    def mark_republish_failed(self, job: RetryJob, error: Exception) -> None:
        """Release a claimed retry job when republishing fails."""


class ReviewAnalysisWorker:
    def __init__(
        self,
        consumer: KafkaConsumer,
        producer: KafkaProducer,
        output_topic: str,
        handler: Callable[[EventEnvelope], dict[str, Any] | None],
        retry_store: RetryStore | None = None,
        retry_policy: RetryPolicy | None = None,
    ) -> None:
        self._consumer = consumer
        self._producer = producer
        self._output_topic = output_topic
        self._handler = handler
        self._retry_store = retry_store
        self._retry_policy = retry_policy or RetryPolicy()

    def run_once(self) -> bool:
        message = self._consumer.poll()
        if message is None:
            return self._publish_due_retry()

        envelope: EventEnvelope | None = None
        try:
            envelope = EventEnvelope.from_dict(message.value)
            result = self._handler(envelope)
            if result is not None:
                self._producer.produce(self._output_topic, result)
        except Exception as exc:
            logger.exception(
                "Failed to process Kafka message from topic %s.", message.topic
            )
            if self._retry_store is None or envelope is None:
                raise
            self._retry_store.record_failure(
                topic=message.topic,
                envelope=envelope,
                error=exc,
                policy=self._retry_policy,
            )
            self._consumer.ack(message)
            return True

        self._consumer.ack(message)
        return True

    def _publish_due_retry(self) -> bool:
        if self._retry_store is None:
            return False
        job = self._retry_store.claim_due()
        if job is None:
            return False
        try:
            self._producer.produce(job.topic, build_retry_envelope(job))
        except Exception as exc:
            self._retry_store.mark_republish_failed(job, exc)
            raise
        self._retry_store.mark_republished(job)
        return True
