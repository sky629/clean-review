# clean-review

`clean-review`는 광고성/협찬성 리뷰로 오염된 맛집, 명소, 제품 리뷰 데이터를 정리해서 실제 사용자 경험 중심의 요약과 신뢰도 지표를 제공하는 리뷰 큐레이션 서비스입니다.

공개 리뷰를 수집하고, 바이럴 오염도를 분석하고, 신뢰도가 낮은 리뷰는 리포트에서 제외합니다. 사용자는 프론트 화면에서 리뷰 타겟 키워드를 등록하고, 수집된 리뷰 요약과 Viral/Quality/Report 상태를 확인할 수 있습니다.

현재 MVP의 핵심 흐름은 다음과 같습니다.

```text
Google OAuth 로그인
  -> 리뷰 타겟 키워드 등록
  -> review.collection.requested 이벤트 발행
  -> 네이버 블로그 리뷰 수집
  -> 리뷰 본문 및 첫 이미지 OCR 텍스트 추출
  -> Gemini/CrewAI 분석
  -> reviews, review_analysis, review_reports 저장
  -> 프론트에서 요약/신뢰도/리포트 포함 여부 확인
  -> 필요 시 수동 Resync 요청
```

## 기술 스택

- 프론트엔드: React, TypeScript, Vite
- 백엔드: Kotlin, Spring Boot, Spring Cloud Gateway, Spring Security, Spring Data JPA
- 워커: Python Kafka-only worker
- 메시징: Kafka
- 저장소: Postgres, Redis
- AI: Gemini `gemini-2.5-flash`, CrewAI workflow 지원
- 로컬 인프라: Docker Compose
- 쿠버네티스 패키징: Helm

## 프로젝트 구조

```text
clean-review/
├── frontend/
├── backend/
│   ├── common/
│   ├── api-gateway/
│   ├── auth-service/
│   ├── review-service/
│   └── notification-service/
├── workers/
│   └── review-analysis-worker/
├── contracts/
│   └── event-contracts/
├── infra/
│   ├── docker-compose/
│   └── helm/
│       └── clean-review/
├── docs/
├── scripts/
├── architecture.md
└── run_local.sh
```

서비스 경계, 코드 구조, 이벤트 흐름은 [architecture.md](./architecture.md)에 정리되어 있습니다.

## 로컬 설정

로컬 실행용 환경변수 파일을 만듭니다.

```bash
cp infra/docker-compose/.env.example infra/docker-compose/.env
```

`infra/docker-compose/.env`에 다음 값들을 채워야 합니다.

- `GOOGLE_CLIENT_ID`
- `GOOGLE_CLIENT_SECRET`
- `GEMINI_API_KEY`
- `NAVER_SEARCH_CLIENT_ID`
- `NAVER_SEARCH_CLIENT_SECRET`
- `JWT_SECRET`

로컬 테스트 시에는 아래 설정을 자주 사용합니다.

```env
REVIEW_ANALYZER_MODE=gemini
REVIEW_COLLECTOR_MAX_REVIEWS_PER_SOURCE=10
```

Google OAuth 로컬 redirect URI:

```text
http://localhost:8080/login/oauth2/code/google
```

네이버 개발자 콘솔 서비스 환경 Web URL:

```text
http://localhost:5173
```

## 로컬 실행

인프라만 실행:

```bash
./run_local.sh up
```

백엔드와 프론트엔드 실행:

```bash
./run_local.sh app
```

리뷰 분석 워커 실행:

```bash
./run_local.sh worker
```

전체 실행:

```bash
./run_local.sh full
```

프론트엔드 접속:

```text
http://localhost:5173/public/review-targets
```

자주 쓰는 로컬 명령:

```bash
./run_local.sh status
./run_local.sh logs
./run_local.sh down
```

로컬 Postgres/Kafka/Redis 볼륨 삭제:

```bash
./run_local.sh clean
```

`clean`은 로컬 데이터를 삭제하는 명령이며 실행 전에 확인 문구를 요구합니다.

## 주요 사용자 흐름

1. `http://localhost:5173/public/review-targets`에 접속합니다.
2. Google OAuth로 로그인합니다.
3. `대돈집` 같은 리뷰 타겟 키워드를 등록합니다.
4. `review-service`가 `ReviewTarget`과 `collection_run`을 생성합니다.
5. `review-service`가 `review.collection.requested` 이벤트를 발행합니다.
6. `review-analysis-worker`가 이벤트를 소비합니다.
7. 워커가 네이버 블로그 검색 API를 날짜순으로 호출합니다.
8. 워커가 후보 블로그 페이지를 Playwright로 열고 본문을 추출합니다.
9. 첫 이미지가 있으면 OCR 텍스트를 리뷰 본문에 추가할 수 있습니다.
10. Gemini/CrewAI 분석 결과를 `review_analysis`에 저장합니다.
11. 타겟 단위 `review_report`를 생성합니다.
12. 프론트엔드에서 summary, Viral, Quality, Report 상태, 원문 링크를 확인합니다.

## 수집 범위

현재 MVP 수집 source:

- `NAVER_BLOG`

현재 런타임에서는 Google Search와 Google Maps 수집을 사용하지 않습니다.

수집 기간:

- 최초 등록: 기본 최근 30일
- 수동 `Resync`: 마지막 성공 완료 시각에서 overlap 시간을 뺀 시점부터 현재까지
- 기본 overlap: 1시간

관련 환경변수:

```env
REVIEW_INITIAL_BACKFILL_DAYS=30
REVIEW_RESYNC_OVERLAP_HOURS=1
REVIEW_COLLECTOR_MAX_REVIEWS_PER_SOURCE=100
```

## 멱등성과 중복 방지

Kafka replay와 중복 이벤트 전달을 전제로 설계합니다.

주요 방어 지점:

- `collection_runs`: 같은 타겟/source의 open run 중복 방지
- `processed_events`: consumer별 이벤트 처리 중복 방지
- `idempotency_records`: 작업 단위 멱등성 기록
- `reviews`: `source + canonical_url_hash`
- `review_analysis`: `review_id + analyzer_version + model_provider + model_name + model_version`
- `notification_deliveries`: `source_event_id + channel + recipient`

수동 `Resync`는 같은 기간이 겹쳐도 이미 저장된 리뷰를 건너뛰고 재분석하지 않습니다.

## 테스트

백엔드:

```bash
cd backend
./gradlew :review-service:test
```

워커:

```bash
cd workers/review-analysis-worker
uv run pytest
uv run black --check src tests
```

프론트엔드:

```bash
cd frontend
npm test -- --run
npm run build
```

## 쿠버네티스

Helm chart 위치:

```text
infra/helm/clean-review/
```

예시 values 파일:

```text
infra/helm/clean-review/values-dev.example.yaml
infra/helm/clean-review/values-prod.example.yaml
```

로컬 `.env`와 Helm values는 별도로 관리합니다. 실제 secret 값은 커밋하지 않습니다.
