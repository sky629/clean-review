# Backend DDD + Clean Architecture

Each Kotlin service follows the same package shape:

```text
domain/
  model/
  event/
  repository/
  service/
application/
  command/
  query/
  usecase/
  port/
    in/
    out/
adapter/
  in/
    web/
      public/
      admin/
    kafka/
  out/
    persistence/
    kafka/
    redis/
    external/
infrastructure/
  config/
  security/
  transaction/
  observability/
```

Rules:

- `domain` does not depend on Spring, JPA, Kafka, HTTP, Redis, or external SDKs.
- Repository interfaces live in `domain/repository`.
- JPA entities, Spring Data repositories, and repository adapters live in `adapter/out/persistence`.
- Controllers and Kafka consumers live in `adapter/in`.
- Kafka producers, Redis clients, Telegram clients, and Google clients live in `adapter/out`.

