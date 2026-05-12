# Kafka Replay and Idempotency

Kafka offset replay is expected. Consumers must be idempotent.

Required guards:

- `processed_events`: `unique(event_id, consumer_name)`
- `idempotency_records`: `unique(scope, idempotency_key)`
- `reviews`: `unique(source, canonical_url_hash)`
- `review_analysis`: `unique(review_id, analyzer_version, model_provider, model_name, model_version)`
- `notification_deliveries`: `unique(source_event_id, channel, recipient)`

Retry uses `retry_jobs` persisted in Postgres. Due jobs are republished to Kafka. Final failures are recorded in `dead_letter_events`.

