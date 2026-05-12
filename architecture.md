# clean-review 아키텍처

이 문서는 첫 커밋 기준의 코드 구조와 서비스 설계를 설명합니다.

## 전체 구조

`clean-review`는 다음 런타임 구성요소를 가진 모노레포입니다.

```text
React frontend
  -> Spring Cloud API Gateway
  -> Kotlin Spring backend services
  -> Kafka events
  -> Python review-analysis-worker
  -> Postgres / Redis
```

백엔드는 서비스 책임별로 나누고, 공통 기술 코드와 이벤트 계약은 `common`, `contracts`에 둡니다.

## 서비스 경계

### `frontend`

React 애플리케이션입니다.

담당 기능:

- Google OAuth 로그인 진입
- 리뷰 타겟 키워드 등록
- 리뷰 목록과 리포트 포함 여부 표시
- 수동 `Resync`
- 관리자용 조회 중심 운영 화면

프론트엔드는 개별 백엔드 서비스를 직접 호출하지 않고 API Gateway를 호출합니다.

### `backend/api-gateway`

외부 HTTP 진입점입니다.

담당 기능:

- `/api/v1/**`, `/admin/api/v1/**` 라우팅
- Gateway 레벨 JWT 검증
- 백엔드 서비스로 요청 전달
- 프론트엔드 CORS 정책 적용

### `backend/auth-service`

인증 bounded context입니다.

담당 기능:

- Google OAuth 로그인
- 일회성 OAuth login code 흐름
- JWT access token 발급
- Redis 기반 refresh token 관리
- 사용자와 소셜 계정 저장

주요 저장소:

- Postgres:
  - `users`
  - `social_accounts`
- Redis:
  - `oauth_login_code:{code_hash}`
  - `refresh_token:{token_hash}`
  - `user_refresh_tokens:{user_id}`

### `backend/review-service`

리뷰 타겟과 운영 데이터 bounded context입니다.

담당 기능:

- 리뷰 타겟 등록, 조회, 수정, 삭제
- 리뷰 타겟 소유자 검증
- 최초 수집 run 생성
- 수동 resync 수집 run 생성
- `review.collection.requested` 이벤트 발행
- 리뷰와 최신 리포트 조회
- 관리자용 조회 중심 운영 API 제공

주요 저장소:

- `review_targets`
- `collection_runs`
- `reviews`
- `review_analysis`
- `review_reports`
- `retry_jobs`
- `dead_letter_events`

### `backend/notification-service`

알림 bounded context입니다.

담당 기능:

- `review.analysis.completed` 이벤트 소비
- 알림 발송 중복 방지
- Telegram 알림 발송

주요 저장소:

- `notification_channels`
- `notification_deliveries`

### `workers/review-analysis-worker`

HTTP API를 열지 않는 Kafka-only Python worker입니다.

담당 기능:

- `review.collection.requested` 이벤트 소비
- 네이버 블로그 검색 API로 후보 URL 수집
- Playwright로 후보 블로그 페이지 열기
- 제목, 본문, 작성일, canonical URL, 첫 이미지 URL 추출
- 첫 이미지가 있으면 OCR 수행
- Gemini 또는 CrewAI로 리뷰 분석
- 협찬/광고 문구에 대한 deterministic rule 적용
- `reviews`, `review_analysis`, `review_reports` 저장
- `review.analysis.completed` 이벤트 발행

현재 MVP 수집 source:

- `NAVER_BLOG`

현재 런타임에서는 Google Search 수집을 사용하지 않습니다.

## 백엔드 패키지 구조

각 Kotlin 서비스는 DDD + Clean Architecture + Hexagonal Architecture 구조를 따릅니다.

이 프로젝트에서 세 아키텍처를 같이 적용하는 방식은 다음과 같습니다.

- DDD는 서비스의 비즈니스 경계와 도메인 모델을 정의합니다.
- Clean Architecture는 코드 의존 방향을 안쪽 도메인으로 향하게 만듭니다.
- Hexagonal Architecture는 application이 외부 시스템을 직접 알지 않도록 port와 adapter로 분리합니다.

핵심 원칙은 `도메인과 유스케이스는 외부 기술을 모른다`입니다. Spring MVC, JPA, Kafka, Redis, Google OAuth, Telegram 같은 기술은 모두 바깥쪽 adapter 또는 infrastructure에 둡니다.

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

규칙:

- `domain`은 비즈니스 개념과 repository interface를 담습니다.
- `domain`은 Spring, JPA, Kafka, Redis, HTTP, 외부 SDK에 의존하지 않습니다.
- `application`은 use case를 오케스트레이션합니다.
- `adapter/in`은 HTTP, Kafka 같은 외부 입력을 받습니다.
- `adapter/out`은 DB, Kafka 발행, Redis, 외부 API 호출을 구현합니다.
- DB 모델은 `adapter/out/persistence`의 JPA entity로 둡니다.
- Controller는 얇게 유지하고 use case에 위임합니다.

## DDD 적용 방식

DDD는 `무엇을 비즈니스 개념으로 볼 것인가`를 정리하는 기준입니다.

현재 주요 bounded context는 다음과 같습니다.

- `auth-service`: 사용자, 소셜 계정, OAuth 로그인, 토큰
- `review-service`: 리뷰 타겟, 수집 run, 리뷰, 분석 결과, 리포트, 운영 이력
- `notification-service`: 알림 채널, 알림 발송 이력
- `review-analysis-worker`: 리뷰 수집, 본문 추출, OCR, LLM 분석, 리포트 생성

도메인 모델 예시:

- `ReviewTarget`
- `CollectionRun`
- `Review`
- `ReviewAnalysis`
- `ReviewReport`
- `User`
- `SocialAccount`
- `NotificationDelivery`

DDD 기준에서 `domain/model`은 단순 DB row가 아닙니다. 비즈니스 규칙을 표현하는 객체입니다. 예를 들어 `ReviewTarget`은 소유자 검증, 삭제 상태, 키워드 변경 같은 규칙을 가질 수 있습니다.

도메인 repository interface는 `domain/repository`에 둡니다. 이유는 repository가 비즈니스 관점에서는 도메인 객체를 저장하고 다시 가져오는 추상 계약이기 때문입니다. 단, 실제 DB 접근 구현체는 도메인이 아니라 adapter에 둡니다.

```text
domain/repository/ReviewTargetRepository.kt
  -> interface

adapter/out/persistence/ReviewTargetPersistenceAdapter.kt
  -> implementation

adapter/out/persistence/ReviewTargetJpaEntity.kt
  -> DB model

adapter/out/persistence/SpringDataReviewTargetJpaRepository.kt
  -> Spring Data JPA repository
```

## Clean Architecture 적용 방식

Clean Architecture의 목적은 비즈니스 규칙을 프레임워크와 인프라 세부사항에서 보호하는 것입니다.

의존 방향은 항상 바깥에서 안쪽으로 향합니다.

```text
infrastructure / adapter
  -> application
    -> domain
```

반대로 아래 방향의 의존은 만들지 않습니다.

```text
domain
  -> application
  -> adapter
  -> infrastructure
```

예를 들어 `ReviewTarget` 도메인 모델은 JPA annotation, Spring annotation, Kafka DTO를 알면 안 됩니다. `RegisterReviewTargetUseCase`는 타겟 등록 규칙과 이벤트 발행 요청을 조율하지만, KafkaTemplate이나 Spring Data JPA를 직접 사용하지 않습니다.

Clean Architecture 관점의 레이어 역할은 다음과 같습니다.

### `domain`

가장 안쪽 레이어입니다.

담당:

- Entity
- Value Object
- Domain Event
- Domain Service
- Repository interface
- 순수 비즈니스 규칙

금지:

- Spring annotation
- JPA entity
- Kafka producer/consumer
- Redis client
- HTTP request/response DTO
- 외부 API SDK

### `application`

유스케이스 레이어입니다.

담당:

- 사용자의 의도를 하나의 use case로 표현
- command/query DTO 정의
- transaction boundary 조율
- domain repository interface 호출
- outbound port 호출

예시:

```text
RegisterReviewTargetUseCase
RequestReviewTargetCollectionUseCase
ListMyReviewTargetReviewsUseCase
GetMyLatestReviewReportUseCase
```

`application`은 비즈니스 흐름은 알지만 구체적인 저장 방식이나 메시지 발행 기술은 모릅니다.

### `adapter`

외부 세계와 application 사이의 변환 레이어입니다.

`adapter/in`:

- REST controller
- Kafka consumer
- 외부 입력 DTO
- 인증 principal 변환

`adapter/out`:

- JPA persistence adapter
- Kafka event publisher
- Redis adapter
- Google OAuth client
- Telegram client

adapter는 외부 기술 객체를 내부 command/query/domain 객체로 변환하거나, 내부 port 호출을 실제 기술 구현으로 연결합니다.

### `infrastructure`

Spring 설정과 기술 wiring 레이어입니다.

담당:

- Security 설정
- Bean 설정
- Transaction 설정
- Observability 설정
- Framework-level configuration

## Hexagonal Architecture 적용 방식

Hexagonal Architecture는 application을 중심에 두고 외부 입출력을 port와 adapter로 분리합니다.

이 프로젝트에서는 다음처럼 해석합니다.

```text
외부 입력
  -> adapter/in
  -> application port/in 또는 usecase
  -> domain
  -> application port/out
  -> adapter/out
  -> 외부 시스템
```

### Inbound Adapter

외부에서 시스템 안으로 들어오는 요청을 받습니다.

예시:

- HTTP Controller
- Kafka Consumer

위치:

```text
adapter/in/web/public
adapter/in/web/admin
adapter/in/kafka
```

역할:

- 인증 정보 확인
- request DTO 검증
- request를 command/query로 변환
- use case 호출
- response DTO로 변환

### Inbound Port

application이 제공하는 기능의 입구입니다.

위치:

```text
application/port/in
```

현재는 많은 경우 use case class를 직접 호출하는 방식으로 충분합니다. 여러 adapter가 같은 use case를 호출하거나 테스트 경계를 더 명확히 해야 할 때 inbound port interface를 둡니다.

### Outbound Port

application이 외부 시스템에 요구하는 작업의 추상 계약입니다.

위치:

```text
application/port/out
```

예시:

- Kafka 이벤트 발행
- Redis 저장
- 외부 OAuth profile 조회
- Telegram 알림 발송

도메인 repository interface는 `domain/repository`에 두고, 기술성 outbound port는 `application/port/out`에 둡니다.

### Outbound Adapter

outbound port나 domain repository interface를 실제 기술로 구현합니다.

위치:

```text
adapter/out/persistence
adapter/out/kafka
adapter/out/redis
adapter/out/external
```

예시:

```text
ReviewTargetPersistenceAdapter
KafkaReviewCollectionEventPublisher
RedisOAuthLoginCodeStore
GoogleOAuthRestClient
TelegramNotificationSender
```

## In Port와 Out Port 차이

`in`과 `out`은 요청의 HTTP 방향이 아니라 application 기준 방향입니다.

`port/in`:

- 외부가 application을 호출하기 위한 입구
- use case가 제공하는 기능
- 예: 리뷰 타겟 등록, 리뷰 조회, 관리자 run 조회

`port/out`:

- application이 외부 기능을 필요로 할 때 사용하는 출구
- 예: 이벤트 발행, 토큰 저장, 외부 OAuth 호출, 알림 발송

즉 “외부에서 요청을 받는다”는 것은 `adapter/in`의 역할이고, `port/out`은 application이 외부 시스템으로 나가기 위한 추상 계약입니다.

## Repository Interface와 구현체 위치

이 프로젝트에서는 repository interface를 도메인에 둡니다.

```text
domain/repository
```

이유:

- 도메인 관점에서 repository는 aggregate를 저장하고 조회하는 계약입니다.
- use case는 도메인 repository interface만 의존합니다.
- 도메인은 JPA, SQL, Spring Data를 알 필요가 없습니다.

구현체는 adapter에 둡니다.

```text
adapter/out/persistence
```

이유:

- JPA Entity, Spring Data Repository, SQL, transaction 같은 세부 기술은 바깥쪽 관심사입니다.
- DB 스키마가 바뀌어도 domain model을 직접 오염시키지 않기 위해서입니다.

정리하면 다음 구조입니다.

```text
domain/repository/ReviewRepository.kt
  <- application/usecase/ListMyReviewTargetReviewsUseCase.kt
  <- adapter/out/persistence/ReviewPersistenceAdapter.kt
       -> SpringDataReviewJpaRepository.kt
       -> ReviewJpaEntity.kt
```

## 워커 패키지 구조

```text
review_analysis_worker/
  analyzers/
    crewai.py
    gemini.py
  collectors/
    collected_review.py
    naver_playwright.py
    page_extractor.py
    search_discovery.py
  runtime/
    app.py
    config.py
    first_image_ocr.py
    gemini.py
    kafka.py
    postgres.py
  collection_pipeline.py
  event_envelope.py
  idempotency.py
  postgres_storage.py
  retry.py
  worker.py
```

주요 경계:

- `runtime/`: 실제 인프라 wiring
- `collectors/`: 후보 URL 수집과 페이지 본문 추출
- `analyzers/`: LLM/CrewAI 리뷰 분석
- `collection_pipeline.py`: 수집, OCR, 분석, 저장, 리포트 생성을 조율
- `postgres_storage.py`: worker 측 Postgres persistence adapter

## 이벤트 흐름

```text
사용자가 ReviewTarget 등록
  -> review-service가 collection_runs row 생성
  -> review-service가 review.collection.requested 발행
  -> review-analysis-worker가 이벤트 소비
  -> worker가 collection run을 RUNNING으로 변경
  -> worker가 네이버 블로그 후보 URL 수집
  -> worker가 리뷰 페이지 본문 추출
  -> worker가 신규 리뷰만 저장
  -> worker가 신규 리뷰만 분석
  -> worker가 review report 생성/갱신
  -> worker가 collection run을 COMPLETED로 변경
  -> worker가 review.analysis.completed 발행
  -> notification-service가 완료 이벤트 소비
```

수동 `Resync`도 같은 흐름을 사용하며 `run_reason=MANUAL_RESYNC`로 구분합니다.

## 수집 기간

최초 등록:

```text
현재 시각 - REVIEW_INITIAL_BACKFILL_DAYS
  -> 현재 시각
```

수동 resync:

```text
마지막 성공 completed_at - REVIEW_RESYNC_OVERLAP_HOURS
  -> 현재 시각
```

성공한 수집 run이 없으면 수동 resync도 최초 수집 기간으로 fallback합니다.

## Kafka 계약

이벤트 스키마 위치:

```text
contracts/event-contracts/
```

주요 이벤트:

- `review.collection.requested.v1`
- `review.collection.completed.v1`
- `review.analysis.completed.v1`

모든 이벤트는 공통 envelope schema를 사용합니다.

## 멱등성 모델

Kafka replay는 정상 상황으로 간주합니다. 중복 작업은 여러 계층에서 막습니다.

- `processed_events`: consumer별 이벤트 중복 처리 방지
- `idempotency_records`: 작업 단위 멱등성 기록
- `collection_runs`: 같은 target/source의 open run 중복 방지
- `reviews`: `source + canonical_url_hash`
- `review_analysis`: 리뷰와 analyzer/model version 기준 unique
- `notification_deliveries`: 알림 발송 unique

이 구조 덕분에 수동 resync가 기간을 겹쳐도 안전합니다. 이미 저장된 리뷰 URL은 분석 전에 건너뜁니다.

## 협찬/광고 판정

분석기는 LLM 결과와 deterministic rule을 함께 사용합니다.

협찬/광고 신호:

- `#협찬`
- `협찬`
- `광고`
- `대가성`
- `제품 제공`
- `무상 제공`
- `원고료`
- `체험단`
- `서포터즈`
- `sponsored`
- `gifted`
- `paid partnership`
- `#ad`

본문 또는 OCR 텍스트에서 해당 신호가 발견되면 다음 규칙을 적용합니다.

- `viral_score`를 강제로 높입니다.
- `quality_score`를 상한값 이하로 제한합니다.
- `useful_for_report=false`로 설정합니다.
- 리뷰는 추적 가능성을 위해 저장하지만 신뢰 리포트에서는 제외합니다.

## API Prefix

일반 인증 API:

```text
/api/v1/**
```

관리자 API:

```text
/admin/api/v1/**
```

관리자 API는 `@AdminOnly` 같은 method-level authorization을 사용합니다.

## 배포 모델

로컬:

- `infra/docker-compose/docker-compose.yml`
- `infra/docker-compose/.env`
- `run_local.sh`

쿠버네티스:

- `infra/helm/clean-review`
- Config와 Secret은 Helm values로 주입합니다.
- 로컬 `.env`는 쿠버네티스 배포에 직접 재사용하지 않습니다.
