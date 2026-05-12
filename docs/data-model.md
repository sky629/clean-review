# Data Model

Postgres tables for MVP:

- `users`
- `social_accounts`
- `categories`
- `review_targets`
- `collection_settings`
- `collection_runs`
- `reviews`
- `review_images`
- `review_image_groups`
- `review_image_group_members`
- `review_analysis`
- `review_reports`
- `idempotency_records`
- `processed_events`
- `retry_jobs`
- `dead_letter_events`
- `notification_channels`
- `notification_deliveries`

`ReviewTarget` supports both places and products through `type = PLACE | PRODUCT`.

