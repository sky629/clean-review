from review_analysis_worker.runtime.kafka import JsonKafkaConsumer, JsonKafkaProducer


class FakeRawMessage:
    def __init__(self, value: bytes, key: bytes | None = None, error=None) -> None:
        self._value = value
        self._key = key
        self._error = error

    def error(self):
        return self._error

    def value(self):
        return self._value

    def key(self):
        return self._key


class FakeConsumer:
    def __init__(self, messages) -> None:
        self.messages = list(messages)
        self.subscribed = []
        self.committed = []

    def subscribe(self, topics):
        self.subscribed = topics

    def poll(self, timeout):
        if not self.messages:
            return None
        return self.messages.pop(0)

    def commit(self, message):
        self.committed.append(message)


class FakeProducer:
    def __init__(self) -> None:
        self.produced = []
        self.flushed = False

    def produce(self, topic, key, value):
        self.produced.append((topic, key, value))

    def flush(self):
        self.flushed = True


def test_json_kafka_consumer_decodes_message_and_commits_ack() -> None:
    raw = FakeRawMessage(b'{"event_id":"evt-1"}', b"target-1")
    inner = FakeConsumer([raw])
    consumer = JsonKafkaConsumer(inner, topic="review.collection.requested")

    message = consumer.poll()
    assert message is not None
    assert message.topic == "review.collection.requested"
    assert message.key == "target-1"
    assert message.value == {"event_id": "evt-1"}

    consumer.ack(message)
    assert inner.subscribed == ["review.collection.requested"]
    assert inner.committed == [raw]


def test_json_kafka_consumer_waits_when_topic_is_not_ready() -> None:
    raw = FakeRawMessage(b"", error=Exception("UNKNOWN_TOPIC_OR_PART"))
    consumer = JsonKafkaConsumer(
        FakeConsumer([raw]), topic="review.collection.requested"
    )

    assert consumer.poll() is None


def test_json_kafka_producer_encodes_dict_payload() -> None:
    inner = FakeProducer()
    producer = JsonKafkaProducer(inner)

    producer.produce("review.analysis.completed", {"event_id": "evt-1"})

    assert inner.produced == [
        ("review.analysis.completed", None, b'{"event_id":"evt-1"}')
    ]
    assert inner.flushed is True
