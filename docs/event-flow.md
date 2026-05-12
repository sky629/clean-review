# Event Flow

```text
ReviewTarget registered
  -> review.collection.requested
  -> review-analysis-worker
  -> reviews + review_analysis + review_reports saved
  -> review.analysis.completed
  -> notification-service
  -> Telegram
```

All events use the common envelope in `contracts/event-contracts/envelope.schema.json`.

