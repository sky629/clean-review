# Event Contracts

All Kafka events use `envelope.schema.json` plus a payload schema under `schemas/`.

Required envelope fields:

- `event_id`
- `event_type`
- `schema_version`
- `occurred_at`
- `correlation_id`
- `idempotency_key`
- `aggregate_id`
- `payload`

Retry events get a new `event_id`, keep the original `idempotency_key`, and include `retry.original_event_id`.

