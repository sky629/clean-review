import pytest

from review_analysis_worker.analyzers.gemini import ReviewAnalysisResult
from review_analysis_worker.collection_pipeline import (
    CollectionPipeline,
    CollectionRunStatus,
    StoredReview,
    StoredReviewAnalysis,
    StoredReviewReport,
)
from review_analysis_worker.collectors.collected_review import CollectedReview
from review_analysis_worker.collectors.errors import CollectionBlocked
from review_analysis_worker.events import EventEnvelope


class FakeCollector:
    def collect(self, *, source: str, keyword: str, **kwargs) -> list[CollectedReview]:
        assert source == "NAVER_BLOG"
        assert keyword == "성수동 파스타 맛집"
        return [
            CollectedReview(
                review_id="naver-101",
                author="summer",
                rating=5,
                created_at="2026-05-06",
                body="떡이 쫄깃했고 포장도 깔끔해서 집에 와서도 먹기 좋았습니다.",
            )
        ]


class SourceEchoCollector:
    def collect(self, *, source: str, keyword: str, **kwargs) -> list[CollectedReview]:
        return [
            CollectedReview(
                review_id=f"{source.lower()}-101",
                author="source-user",
                rating=None,
                created_at="2026-05-08",
                body=f"{source}에서 수집한 실제 사용 후기입니다.",
                canonical_url=f"https://example.com/{source.lower()}-101",
            )
        ]


class FakeAnalyzer:
    model = "gemini-2.5-flash"

    def analyze(self, request):
        return {
            "viral_score": 12.0,
            "quality_score": 91.0,
            "summary": "구체적인 구매 경험이 있는 신뢰도 높은 후기입니다.",
            "pros": ["포장 상태가 구체적으로 언급됨"],
            "cons": [],
            "detected_patterns": [],
            "evidence": ["떡이 쫄깃했고 포장도 깔끔"],
        }


class NeutralAnalyzer:
    model = "gemini-2.5-flash"

    def analyze(self, request):
        return {
            "viral_score": 5.0,
            "quality_score": 90.0,
            "summary": "구체적인 방문 경험이 있는 후기입니다.",
            "pros": [],
            "cons": [],
            "detected_patterns": [],
            "evidence": [],
        }


class FirstImageCollector:
    def collect(self, *, source: str, keyword: str, **kwargs) -> list[CollectedReview]:
        return [
            CollectedReview(
                review_id="first-image-sponsored",
                author="ad-user",
                rating=5,
                created_at="2026-05-06",
                body="파스타 맛이 좋았고 매장이 깔끔했습니다.",
                first_image_url="https://example.com/first-image.png",
            )
        ]


class FakeFirstImageOcr:
    def __init__(self) -> None:
        self.calls: list[str | None] = []

    def extract_text(self, image_url: str | None) -> str:
        self.calls.append(image_url)
        return "#협찬"


class FailingAnalyzer:
    model = "gemini-2.5-flash"

    def analyze(self, request):
        raise RuntimeError("gemini failed")


class SlottedResultAnalyzer:
    model = "gemini-2.5-flash"

    def analyze(self, request):
        return ReviewAnalysisResult(
            sentiment="positive",
            summary="구체적인 구매 경험이 있는 신뢰도 높은 후기입니다.",
            topics=["포장"],
            viral_score=12.0,
            quality_score=91.0,
            is_suspicious=False,
            useful_for_report=True,
            pros=["포장 상태가 구체적으로 언급됨"],
            cons=[],
            detected_patterns=[],
            evidence=["떡이 쫄깃했고 포장도 깔끔"],
        )


class MixedReviewCollector:
    def collect(self, *, source: str, keyword: str, **kwargs) -> list[CollectedReview]:
        return [
            CollectedReview(
                review_id="real-101",
                author="real-user",
                rating=5,
                created_at="2026-05-06",
                body="웨이팅은 20분이었고 포장 상태가 좋았습니다.",
            ),
            CollectedReview(
                review_id="viral-999",
                author="ad-user",
                rating=5,
                created_at="2026-05-06",
                body="협찬받아 방문했어요. 성수동 파스타 맛집 최고!",
            ),
        ]


class CanonicalUrlCollector:
    def collect(self, *, source: str, keyword: str, **kwargs) -> list[CollectedReview]:
        return [
            CollectedReview(
                review_id="naver-real-101",
                author="real-user",
                rating=5,
                created_at="2026-05-06",
                body="웨이팅은 20분 정도였고 소스가 진해서 좋았습니다.",
                canonical_url="https://blog.naver.com/example/223",
            )
        ]


class BlockedCollector:
    def collect(self, *, source: str, keyword: str, **kwargs) -> list[CollectedReview]:
        raise CollectionBlocked("collection blocked for NAVER_BLOG")


class MixedReviewAnalyzer:
    model = "gemini-2.5-flash"

    def analyze(self, request):
        if "협찬" in request.body:
            return {
                "viral_score": 94.0,
                "quality_score": 18.0,
                "detected_patterns": ["paid_disclosure", "keyword_repetition"],
                "evidence": ["협찬받아 방문"],
            }
        return {
            "viral_score": 8.0,
            "quality_score": 88.0,
            "detected_patterns": [],
            "evidence": ["웨이팅은 20분"],
        }


class FakeStorage:
    def __init__(self) -> None:
        self.collection_statuses: list[tuple[str, CollectionRunStatus]] = []
        self.reviews = []
        self.analyses: list[StoredReviewAnalysis] = []
        self.reports: list[StoredReviewReport] = []

    def mark_collection_running(self, collection_run_id: str) -> bool:
        self.collection_statuses.append(
            (collection_run_id, CollectionRunStatus.RUNNING)
        )
        return True

    def find_review_by_source_and_url_hash(
        self, *, source: str, canonical_url_hash: str
    ):
        return None

    def analysis_exists(self, **kwargs) -> bool:
        return False

    def save_review(self, review):
        self.reviews.append(review)
        return review

    def save_analysis(self, analysis: StoredReviewAnalysis):
        self.analyses.append(analysis)
        return analysis

    def save_report(self, report: StoredReviewReport):
        self.reports.append(report)
        return report

    def mark_collection_completed(self, collection_run_id: str) -> None:
        self.collection_statuses.append(
            (collection_run_id, CollectionRunStatus.COMPLETED)
        )

    def mark_collection_failed(
        self, collection_run_id: str, failure_code: str, failure_message: str
    ) -> None:
        self.collection_statuses.append((collection_run_id, CollectionRunStatus.FAILED))
        self.failure = (collection_run_id, failure_code, failure_message)


def test_collection_pipeline_saves_reviews_analysis_report_and_completion_event() -> (
    None
):
    storage = FakeStorage()
    pipeline = CollectionPipeline(
        collector=FakeCollector(),
        analyzer=FakeAnalyzer(),
        storage=storage,
    )
    event = EventEnvelope.from_dict(
        {
            "event_id": "evt-1",
            "event_type": "review.collection.requested.v1",
            "schema_version": 1,
            "occurred_at": "2026-05-07T03:15:00Z",
            "correlation_id": "corr-1",
            "idempotency_key": "review-collection-request:target-1",
            "aggregate_id": "target-1",
            "payload": {
                "collection_run_id": "run-1",
                "target_id": "target-1",
                "source": "NAVER_BLOG",
                "keyword": "성수동 파스타 맛집",
                "requested_by": "user-1",
                "window_from": "2026-04-11T12:00:00Z",
                "window_to": "2026-05-11T12:00:00Z",
                "max_reviews": 100,
                "run_reason": "INITIAL_BACKFILL",
            },
        }
    )

    completed = pipeline.handle(event)

    assert storage.collection_statuses == [
        ("run-1", CollectionRunStatus.RUNNING),
        ("run-1", CollectionRunStatus.COMPLETED),
    ]
    assert storage.reviews[0].target_id == "target-1"
    assert storage.reviews[0].source_review_id == "naver-101"
    assert storage.reviews[0].canonical_url_hash
    assert storage.reviews[0].normalized_text_hash
    assert storage.analyses[0].review_id == storage.reviews[0].id
    assert storage.analyses[0].model_name == "gemini-2.5-flash"
    assert (
        storage.analyses[0].summary
        == "구체적인 구매 경험이 있는 신뢰도 높은 후기입니다."
    )
    assert storage.reports == [
        StoredReviewReport(
            id=storage.reports[0].id,
            target_id="target-1",
            collection_run_id="run-1",
            analyzer_version="review-analysis-worker-0.1.0",
            model_provider="google",
            model_name="gemini-2.5-flash",
            model_version="gemini-2.5-flash",
            viral_contamination_score=12.0,
            trust_score=91.0,
            summary="구체적인 구매 경험이 있는 신뢰도 높은 후기입니다.",
            pros=["포장 상태가 구체적으로 언급됨"],
            cons=[],
            evidence_review_ids=[storage.reviews[0].id],
            report_hash=storage.reports[0].report_hash,
        )
    ]
    assert completed["event_type"] == "review.analysis.completed.v1"
    assert completed["aggregate_id"] == "target-1"
    assert completed["payload"]["collection_run_id"] == "run-1"
    assert completed["payload"]["report_id"] == storage.reports[0].id


def test_collection_pipeline_appends_first_image_ocr_before_analysis() -> None:
    storage = FakeStorage()
    ocr = FakeFirstImageOcr()
    pipeline = CollectionPipeline(
        collector=FirstImageCollector(),
        analyzer=NeutralAnalyzer(),
        storage=storage,
        first_image_ocr=ocr,
    )
    event = EventEnvelope.from_dict(
        {
            "event_id": "evt-1",
            "event_type": "review.collection.requested.v1",
            "schema_version": 1,
            "occurred_at": "2026-05-07T03:15:00Z",
            "correlation_id": "corr-1",
            "idempotency_key": "review-collection-request:target-1",
            "aggregate_id": "target-1",
            "payload": {
                "collection_run_id": "run-1",
                "target_id": "target-1",
                "source": "NAVER_BLOG",
                "keyword": "성수동 파스타 맛집",
            },
        }
    )

    pipeline.handle(event)

    assert ocr.calls == ["https://example.com/first-image.png"]
    assert "첫 이미지 OCR 텍스트: #협찬" in storage.reviews[0].raw_text


class DuplicateSkippingStorage(FakeStorage):
    def __init__(self) -> None:
        super().__init__()
        self.existing_review = StoredReview(
            id="existing-review-1",
            target_id="target-1",
            collection_run_id="previous-run",
            source="NAVER_BLOG",
            source_review_id="naver-101",
            canonical_url="https://blog.naver.com/reviews/naver-101",
            canonical_url_hash="existing-url-hash",
            normalized_text_hash="existing-text-hash",
            author_hash="existing-author-hash",
            raw_text="이미 수집된 리뷰입니다.",
            published_at="2026-05-06",
        )

    def find_review_by_source_and_url_hash(
        self, *, source: str, canonical_url_hash: str
    ):
        return self.existing_review

    def analysis_exists(self, **kwargs) -> bool:
        return True

    def save_review(self, review):
        self.reviews.append(review)
        return None


class CompletedRunStorage(FakeStorage):
    def mark_collection_running(self, collection_run_id: str) -> bool:
        self.collection_statuses.append(
            (collection_run_id, CollectionRunStatus.COMPLETED)
        )
        return False


def test_collection_pipeline_skips_analysis_for_duplicate_reviews() -> None:
    storage = DuplicateSkippingStorage()
    pipeline = CollectionPipeline(
        collector=FakeCollector(),
        analyzer=FakeAnalyzer(),
        storage=storage,
    )
    event = EventEnvelope.from_dict(
        {
            "event_id": "evt-1",
            "event_type": "review.collection.requested.v1",
            "schema_version": 1,
            "occurred_at": "2026-05-07T03:15:00Z",
            "correlation_id": "corr-1",
            "idempotency_key": "review-collection-request:target-1",
            "aggregate_id": "target-1",
            "payload": {
                "collection_run_id": "run-1",
                "target_id": "target-1",
                "source": "NAVER_BLOG",
                "keyword": "성수동 파스타 맛집",
                "window_from": "2026-04-11T12:00:00Z",
                "window_to": "2026-05-11T12:00:00Z",
                "max_reviews": 100,
                "run_reason": "MANUAL_RESYNC",
            },
        }
    )

    pipeline.handle(event)

    assert storage.reviews == []
    assert storage.analyses == []
    assert storage.reports == []
    assert storage.collection_statuses[-1] == ("run-1", CollectionRunStatus.COMPLETED)


def test_collection_pipeline_skips_first_image_ocr_for_duplicate_reviews() -> None:
    storage = DuplicateSkippingStorage()
    ocr = FakeFirstImageOcr()
    pipeline = CollectionPipeline(
        collector=FirstImageCollector(),
        analyzer=NeutralAnalyzer(),
        storage=storage,
        first_image_ocr=ocr,
    )
    event = EventEnvelope.from_dict(
        {
            "event_id": "evt-1",
            "event_type": "review.collection.requested.v1",
            "schema_version": 1,
            "occurred_at": "2026-05-07T03:15:00Z",
            "correlation_id": "corr-1",
            "idempotency_key": "review-collection-request:target-1",
            "aggregate_id": "target-1",
            "payload": {
                "collection_run_id": "run-1",
                "target_id": "target-1",
                "source": "NAVER_BLOG",
                "keyword": "성수동 파스타 맛집",
            },
        }
    )

    assert pipeline.handle(event) is None
    assert ocr.calls == []
    assert storage.analyses == []


def test_collection_pipeline_noops_when_collection_run_is_already_completed() -> None:
    storage = CompletedRunStorage()
    pipeline = CollectionPipeline(
        collector=FakeCollector(),
        analyzer=FakeAnalyzer(),
        storage=storage,
    )
    event = EventEnvelope.from_dict(
        {
            "event_id": "evt-1",
            "event_type": "review.collection.requested.v1",
            "schema_version": 1,
            "occurred_at": "2026-05-07T03:15:00Z",
            "correlation_id": "corr-1",
            "idempotency_key": "review-collection-request:target-1",
            "aggregate_id": "target-1",
            "payload": {
                "collection_run_id": "run-1",
                "target_id": "target-1",
                "source": "NAVER_BLOG",
                "keyword": "성수동 파스타 맛집",
            },
        }
    )

    assert pipeline.handle(event) is None
    assert storage.reviews == []
    assert storage.analyses == []
    assert storage.reports == []


def test_collection_pipeline_handles_naver_blog_source() -> None:
    storage = FakeStorage()
    pipeline = CollectionPipeline(
        collector=SourceEchoCollector(),
        analyzer=FakeAnalyzer(),
        storage=storage,
    )
    event = EventEnvelope.from_dict(
        {
            "event_id": "evt-NAVER_BLOG",
            "event_type": "review.collection.requested.v1",
            "schema_version": 1,
            "occurred_at": "2026-05-07T03:15:00Z",
            "correlation_id": "corr-NAVER_BLOG",
            "idempotency_key": "review-collection-request:target-1:NAVER_BLOG",
            "aggregate_id": "target-1",
            "payload": {
                "collection_run_id": "run-NAVER_BLOG",
                "target_id": "target-1",
                "source": "NAVER_BLOG",
                "keyword": "무선 이어폰 후기",
            },
        }
    )

    completed = pipeline.handle(event)

    assert storage.reviews[0].source == "NAVER_BLOG"
    assert storage.reviews[0].canonical_url == "https://example.com/naver_blog-101"
    assert completed["payload"]["collection_run_id"] == "run-NAVER_BLOG"


def test_collection_pipeline_accepts_slotted_analysis_result_objects() -> None:
    storage = FakeStorage()
    pipeline = CollectionPipeline(
        collector=FakeCollector(),
        analyzer=SlottedResultAnalyzer(),
        storage=storage,
    )
    event = EventEnvelope.from_dict(
        {
            "event_id": "evt-1",
            "event_type": "review.collection.requested.v1",
            "schema_version": 1,
            "occurred_at": "2026-05-07T03:15:00Z",
            "correlation_id": "corr-1",
            "idempotency_key": "review-collection-request:target-1",
            "aggregate_id": "target-1",
            "payload": {
                "collection_run_id": "run-1",
                "target_id": "target-1",
                "source": "NAVER_BLOG",
                "keyword": "성수동 파스타 맛집",
            },
        }
    )

    pipeline.handle(event)

    assert storage.analyses[0].viral_score == 12.0
    assert storage.analyses[0].quality_score == 91.0
    assert storage.analyses[0].evidence == ["떡이 쫄깃했고 포장도 깔끔"]
    assert storage.collection_statuses[-1] == ("run-1", CollectionRunStatus.COMPLETED)


def test_collection_pipeline_preserves_collected_canonical_url() -> None:
    storage = FakeStorage()
    pipeline = CollectionPipeline(
        collector=CanonicalUrlCollector(),
        analyzer=FakeAnalyzer(),
        storage=storage,
    )
    event = EventEnvelope.from_dict(
        {
            "event_id": "evt-1",
            "event_type": "review.collection.requested.v1",
            "schema_version": 1,
            "occurred_at": "2026-05-07T03:15:00Z",
            "correlation_id": "corr-1",
            "idempotency_key": "review-collection-request:target-1",
            "aggregate_id": "target-1",
            "payload": {
                "collection_run_id": "run-1",
                "target_id": "target-1",
                "source": "NAVER_BLOG",
                "keyword": "성수동 파스타 맛집",
            },
        }
    )

    pipeline.handle(event)

    assert storage.reviews[0].canonical_url == "https://blog.naver.com/example/223"


def test_collection_pipeline_keeps_original_reviews_but_excludes_viral_reviews_from_report() -> (
    None
):
    storage = FakeStorage()
    pipeline = CollectionPipeline(
        collector=MixedReviewCollector(),
        analyzer=MixedReviewAnalyzer(),
        storage=storage,
    )
    event = EventEnvelope.from_dict(
        {
            "event_id": "evt-1",
            "event_type": "review.collection.requested.v1",
            "schema_version": 1,
            "occurred_at": "2026-05-07T03:15:00Z",
            "correlation_id": "corr-1",
            "idempotency_key": "review-collection-request:target-1",
            "aggregate_id": "target-1",
            "payload": {
                "collection_run_id": "run-1",
                "target_id": "target-1",
                "source": "NAVER_BLOG",
                "keyword": "성수동 파스타 맛집",
            },
        }
    )

    pipeline.handle(event)

    assert len(storage.reviews) == 2
    assert [analysis.is_suspicious for analysis in storage.analyses] == [False, True]
    assert [analysis.useful_for_report for analysis in storage.analyses] == [
        True,
        False,
    ]
    assert storage.reports[0].evidence_review_ids == [storage.reviews[0].id]
    assert storage.reports[0].trust_score == 88.0
    assert storage.reports[0].viral_contamination_score == 51.0


def test_collection_pipeline_marks_collection_failed_before_retry_on_blocked_collection() -> (
    None
):
    storage = FakeStorage()
    pipeline = CollectionPipeline(
        collector=BlockedCollector(),
        analyzer=FakeAnalyzer(),
        storage=storage,
    )
    event = EventEnvelope.from_dict(
        {
            "event_id": "evt-1",
            "event_type": "review.collection.requested.v1",
            "schema_version": 1,
            "occurred_at": "2026-05-07T03:15:00Z",
            "correlation_id": "corr-1",
            "idempotency_key": "review-collection-request:target-1",
            "aggregate_id": "target-1",
            "payload": {
                "collection_run_id": "run-1",
                "target_id": "target-1",
                "source": "NAVER_BLOG",
                "keyword": "무선 이어폰 후기",
            },
        }
    )

    with pytest.raises(CollectionBlocked):
        pipeline.handle(event)

    assert storage.collection_statuses == [
        ("run-1", CollectionRunStatus.RUNNING),
        ("run-1", CollectionRunStatus.FAILED),
    ]
    assert storage.failure == (
        "run-1",
        "CollectionBlocked",
        "collection blocked for NAVER_BLOG",
    )


def test_collection_pipeline_marks_collection_failed_when_analysis_fails() -> None:
    storage = FakeStorage()
    pipeline = CollectionPipeline(
        collector=FakeCollector(),
        analyzer=FailingAnalyzer(),
        storage=storage,
    )
    event = EventEnvelope.from_dict(
        {
            "event_id": "evt-1",
            "event_type": "review.collection.requested.v1",
            "schema_version": 1,
            "occurred_at": "2026-05-07T03:15:00Z",
            "correlation_id": "corr-1",
            "idempotency_key": "review-collection-request:target-1",
            "aggregate_id": "target-1",
            "payload": {
                "collection_run_id": "run-1",
                "target_id": "target-1",
                "source": "NAVER_BLOG",
                "keyword": "성수동 파스타 맛집",
            },
        }
    )

    with pytest.raises(RuntimeError):
        pipeline.handle(event)

    assert storage.collection_statuses == [
        ("run-1", CollectionRunStatus.RUNNING),
        ("run-1", CollectionRunStatus.FAILED),
    ]
    assert storage.failure == ("run-1", "RuntimeError", "gemini failed")
    assert storage.reports == []
