from __future__ import annotations

import logging
from collections.abc import Callable
from dataclasses import dataclass
from typing import Any, Protocol

from review_analysis_worker.events import EventEnvelope

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


class ReviewAnalysisWorker:
    def __init__(
        self,
        consumer: KafkaConsumer,
        producer: KafkaProducer,
        output_topic: str,
        handler: Callable[[EventEnvelope], dict[str, Any]],
    ) -> None:
        self._consumer = consumer
        self._producer = producer
        self._output_topic = output_topic
        self._handler = handler

    def run_once(self) -> bool:
        message = self._consumer.poll()
        if message is None:
            return False

        try:
            envelope = EventEnvelope.from_dict(message.value)
            result = self._handler(envelope)
            self._producer.produce(self._output_topic, result)
        except Exception:
            logger.exception(
                "Failed to process Kafka message from topic %s.", message.topic
            )
            self._consumer.ack(message)
            return True

        self._consumer.ack(message)
        return True
