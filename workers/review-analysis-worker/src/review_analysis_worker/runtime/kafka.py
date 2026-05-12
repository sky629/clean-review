from __future__ import annotations

import json
from typing import Any

from review_analysis_worker.worker import KafkaMessage


class KafkaRuntimeDependencyError(RuntimeError):
    pass


class JsonKafkaConsumer:
    def __init__(
        self, consumer: Any, *, topic: str, poll_timeout_seconds: float = 1.0
    ) -> None:
        self._consumer = consumer
        self._topic = topic
        self._poll_timeout_seconds = poll_timeout_seconds
        self._message_index: dict[int, Any] = {}
        self._consumer.subscribe([topic])

    def poll(self) -> KafkaMessage | None:
        raw = self._consumer.poll(self._poll_timeout_seconds)
        if raw is None:
            return None
        if raw.error():
            error = str(raw.error())
            if "UNKNOWN_TOPIC_OR_PART" in error:
                return None
            raise RuntimeError(error)

        key = raw.key().decode("utf-8") if raw.key() is not None else None
        message = KafkaMessage(
            topic=self._topic,
            key=key,
            value=json.loads(raw.value().decode("utf-8")),
        )
        self._message_index[id(message)] = raw
        return message

    def ack(self, message: KafkaMessage) -> None:
        raw = self._message_index.pop(id(message))
        self._consumer.commit(raw)


class JsonKafkaProducer:
    def __init__(self, producer: Any) -> None:
        self._producer = producer

    def produce(self, topic: str, value: dict[str, Any]) -> None:
        self._producer.produce(
            topic,
            key=value.get("aggregate_id"),
            value=json.dumps(value, ensure_ascii=False, separators=(",", ":")).encode(
                "utf-8"
            ),
        )
        self._producer.flush()


def create_confluent_consumer(*, bootstrap_servers: str, group_id: str):
    try:
        from confluent_kafka import Consumer
    except ImportError as exc:
        raise KafkaRuntimeDependencyError(
            "confluent-kafka is required to run the worker against Kafka."
        ) from exc

    return Consumer(
        {
            "bootstrap.servers": bootstrap_servers,
            "group.id": group_id,
            "auto.offset.reset": "earliest",
            "enable.auto.commit": False,
        }
    )


def create_confluent_producer(*, bootstrap_servers: str):
    try:
        from confluent_kafka import Producer
    except ImportError as exc:
        raise KafkaRuntimeDependencyError(
            "confluent-kafka is required to run the worker against Kafka."
        ) from exc

    return Producer({"bootstrap.servers": bootstrap_servers})
