# Architecture

`clean-review` uses a monorepo with a React frontend, Kotlin Spring microservices, a Python Kafka-only review analysis worker, and Kafka/Postgres/Redis infrastructure.

## Service Boundaries

- `api-gateway`: external HTTP entrypoint, Spring Cloud Gateway routes, JWT verification, coarse authorization.
- `auth-service`: Google OAuth, JWT issuing, Redis-backed refresh tokens and OAuth login codes.
- `review-service`: review targets, categories, reviews, collection runs, analysis runs, reports, admin read-only operations.
- `notification-service`: consumes analysis completion events and sends Telegram notifications.
- `review-analysis-worker`: consumes collection requests, crawls/parses reviews, deduplicates, analyzes with Gemini, writes results, emits completion events.

## API Prefixes

- Public authenticated APIs: `/api/v1/**`
- Admin APIs: `/admin/api/v1/**`

## Security

Gateway and each Kotlin service verify JWTs. Controllers receive `AuthenticatedUser` through Spring Security and use `@AdminOnly` for admin-only handlers.

## Persistence

MVP uses one Postgres schema with clear table ownership by service. Redis stores authentication session state:

- `oauth_login_code:{code_hash}`
- `refresh_token:{token_hash}`
- `user_refresh_tokens:{user_id}`

