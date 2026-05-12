from __future__ import annotations

import hashlib
import uuid
from dataclasses import asdict, dataclass, is_dataclass
from datetime import UTC, datetime
from enum import Enum
from typing import Any, Protocol

from review_analysis_worker.collectors.collected_review import CollectedReview
from review_analysis_worker.events import EventEnvelope

ANALYZER_VERSION = "review-analysis-worker-0.1.0"
MODEL_PROVIDER = "google"
VIRAL_EXCLUSION_THRESHOLD = 70.0
MIN_REPORT_QUALITY_SCORE = 50.0


class CollectionRunStatus(Enum):
    RUNNING = "RUNNING"
    COMPLETED = "COMPLETED"
    FAILED = "FAILED"


@dataclass(frozen=True, slots=True)
class AnalysisRequest:
    review_id: str
    body: str
    rating: int | None


@dataclass(frozen=True, slots=True)
class StoredReview:
    id: str
    target_id: str
    collection_run_id: str
    source: str
    source_review_id: str
    canonical_url: str
    canonical_url_hash: str
    normalized_text_hash: str
    author_hash: str
    raw_text: str
    published_at: str
    status: str = "COLLECTED"


@dataclass(frozen=True, slots=True)
class StoredReviewAnalysis:
    id: str
    review_id: str
    collection_run_id: str
    analyzer_version: str
    model_provider: str
    model_name: str
    model_version: str
    viral_score: float
    quality_score: float
    is_suspicious: bool
    useful_for_report: bool
    summary: str
    detected_patterns: list[str]
    evidence: list[str]
    status: str = "COMPLETED"


@dataclass(frozen=True, slots=True)
class StoredReviewReport:
    id: str
    target_id: str
    collection_run_id: str
    analyzer_version: str
    model_provider: str
    model_name: str
    model_version: str
    viral_contamination_score: float
    trust_score: float
    summary: str
    pros: list[str]
    cons: list[str]
    evidence_review_ids: list[str]
    report_hash: str


class ReviewCollector(Protocol):
    def collect(
        self,
        *,
        source: str,
        keyword: str,
        window_from: datetime | None = None,
        window_to: datetime | None = None,
        max_reviews: int | None = None,
    ) -> list[CollectedReview]:
        """Collect raw reviews for a review target."""


class ReviewAnalyzer(Protocol):
    model: str

    def analyze(self, request: Any) -> Any:
        """Analyze a collected review."""


class FirstImageOcr(Protocol):
    def extract_text(self, image_url: str | None) -> str:
        """Extract OCR text from the first review image when available."""


class ReviewStorage(Protocol):
    def mark_collection_running(self, collection_run_id: str) -> None:
        """Mark a collection run as running."""

    def save_review(self, review: StoredReview) -> StoredReview | None:
        """Save a collected review idempotently."""

    def save_analysis(self, analysis: StoredReviewAnalysis) -> StoredReviewAnalysis:
        """Save review analysis idempotently."""

    def save_report(self, report: StoredReviewReport) -> StoredReviewReport:
        """Save the aggregated report idempotently."""

    def mark_collection_completed(self, collection_run_id: str) -> None:
        """Mark a collection run as completed."""

    def mark_collection_failed(
        self, collection_run_id: str, failure_code: str, failure_message: str
    ) -> None:
        """Mark a collection run as failed."""


class UnsupportedCollectionEvent(ValueError):
    pass


class CollectionPipeline:
    def __init__(
        self,
        collector: ReviewCollector,
        analyzer: ReviewAnalyzer,
        storage: ReviewStorage,
        first_image_ocr: FirstImageOcr | None = None,
    ) -> None:
        self._collector = collector
        self._analyzer = analyzer
        self._storage = storage
        self._first_image_ocr = first_image_ocr

    @property
    def collector_name(self) -> str:
        return type(self._collector).__name__

    def handle(self, event: EventEnvelope) -> dict[str, Any]:
        if event.event_type != "review.collection.requested.v1":
            raise UnsupportedCollectionEvent(event.event_type)

        payload = event.payload
        collection_run_id = _required(payload, "collection_run_id")
        target_id = _required(payload, "target_id")
        source = _required(payload, "source")
        keyword = _required(payload, "keyword")
        window_from = _optional_datetime(payload.get("window_from"))
        window_to = _optional_datetime(payload.get("window_to"))
        max_reviews = int(payload.get("max_reviews") or 20)
        model_name = getattr(self._analyzer, "model", "gemini-2.5-flash")

        self._storage.mark_collection_running(collection_run_id)
        try:
            reviews = self._collector.collect(
                source=source,
                keyword=keyword,
                window_from=window_from,
                window_to=window_to,
                max_reviews=max_reviews,
            )

            stored_reviews: list[StoredReview] = []
            analyses: list[StoredReviewAnalysis] = []

            for raw_review in reviews:
                raw_review = _with_first_image_ocr(raw_review, self._first_image_ocr)
                stored_review = self._storage.save_review(
                    _to_stored_review(
                        raw_review,
                        target_id=target_id,
                        collection_run_id=collection_run_id,
                        source=source,
                    )
                )
                if stored_review is None:
                    continue
                stored_reviews.append(stored_review)

                analysis_result = self._analyzer.analyze(
                    AnalysisRequest(
                        review_id=stored_review.id,
                        body=stored_review.raw_text,
                        rating=raw_review.rating,
                    )
                )
                analyses.append(
                    self._storage.save_analysis(
                        _to_stored_analysis(
                            analysis_result,
                            review_id=stored_review.id,
                            collection_run_id=collection_run_id,
                            model_name=model_name,
                        )
                    )
                )

            report = self._storage.save_report(
                _build_report(
                    target_id=target_id,
                    collection_run_id=collection_run_id,
                    model_name=model_name,
                    reviews=stored_reviews,
                    analyses=analyses,
                )
            )
            self._storage.mark_collection_completed(collection_run_id)
        except Exception as exc:
            self._storage.mark_collection_failed(
                collection_run_id,
                type(exc).__name__,
                str(exc),
            )
            raise

        return _completion_event(
            correlation_id=event.correlation_id or event.event_id,
            target_id=target_id,
            collection_run_id=collection_run_id,
            report=report,
        )


def _required(payload: dict[str, Any], key: str) -> str:
    value = payload.get(key)
    if not isinstance(value, str) or not value.strip():
        raise ValueError(f"{key} is required")
    return value


def _optional_datetime(value: Any) -> datetime | None:
    if not isinstance(value, str) or not value.strip():
        return None
    return datetime.fromisoformat(value.replace("Z", "+00:00"))


def _to_stored_review(
    review: CollectedReview,
    *,
    target_id: str,
    collection_run_id: str,
    source: str,
) -> StoredReview:
    normalized_text = _normalize_text(review.body)
    canonical_url = (
        review.canonical_url or f"https://blog.naver.com/reviews/{review.review_id}"
    )
    return StoredReview(
        id=str(uuid.uuid5(uuid.NAMESPACE_URL, f"{source}:{review.review_id}")),
        target_id=target_id,
        collection_run_id=collection_run_id,
        source=source,
        source_review_id=review.review_id,
        canonical_url=canonical_url,
        canonical_url_hash=_sha256(canonical_url),
        normalized_text_hash=_sha256(normalized_text),
        author_hash=_sha256(review.author),
        raw_text=review.body,
        published_at=review.created_at,
    )


def _with_first_image_ocr(
    review: CollectedReview, first_image_ocr: FirstImageOcr | None
) -> CollectedReview:
    if first_image_ocr is None or not review.first_image_url:
        return review
    ocr_text = first_image_ocr.extract_text(review.first_image_url).strip()
    if not ocr_text:
        return review
    return CollectedReview(
        review_id=review.review_id,
        author=review.author,
        rating=review.rating,
        created_at=review.created_at,
        body="\n\n".join([review.body, f"첫 이미지 OCR 텍스트: {ocr_text}"]),
        canonical_url=review.canonical_url,
        first_image_url=review.first_image_url,
    )


def _to_stored_analysis(
    result: Any,
    *,
    review_id: str,
    collection_run_id: str,
    model_name: str,
) -> StoredReviewAnalysis:
    result_map = _analysis_result_to_map(result)
    viral_score = float(result_map.get("viral_score", 0.0))
    quality_score = float(result_map.get("quality_score", 0.0))
    detected_patterns = [str(item) for item in result_map.get("detected_patterns", [])]
    is_suspicious = (
        bool(result_map.get("is_suspicious", False))
        or viral_score >= VIRAL_EXCLUSION_THRESHOLD
    )
    useful_for_report = (
        bool(result_map.get("useful_for_report", True))
        and not is_suspicious
        and quality_score >= MIN_REPORT_QUALITY_SCORE
    )
    return StoredReviewAnalysis(
        id=str(
            uuid.uuid5(
                uuid.NAMESPACE_URL, f"{review_id}:{ANALYZER_VERSION}:{model_name}"
            )
        ),
        review_id=review_id,
        collection_run_id=collection_run_id,
        analyzer_version=ANALYZER_VERSION,
        model_provider=MODEL_PROVIDER,
        model_name=model_name,
        model_version=model_name,
        viral_score=viral_score,
        quality_score=quality_score,
        is_suspicious=is_suspicious,
        useful_for_report=useful_for_report,
        summary=str(result_map.get("summary", "")).strip(),
        detected_patterns=detected_patterns,
        evidence=[str(item) for item in result_map.get("evidence", [])],
    )


def _analysis_result_to_map(result: Any) -> dict[str, Any]:
    if isinstance(result, dict):
        return result
    if is_dataclass(result):
        return asdict(result)
    if hasattr(result, "model_dump"):
        return result.model_dump()
    if hasattr(result, "__dict__"):
        return result.__dict__
    raise TypeError(f"Unsupported analysis result type: {type(result).__name__}")


def _build_report(
    *,
    target_id: str,
    collection_run_id: str,
    model_name: str,
    reviews: list[StoredReview],
    analyses: list[StoredReviewAnalysis],
) -> StoredReviewReport:
    analysis_by_review_id = {analysis.review_id: analysis for analysis in analyses}
    report_reviews = [
        review
        for review in reviews
        if analysis_by_review_id.get(review.id) is not None
        and analysis_by_review_id[review.id].useful_for_report
    ]
    report_analyses = [analysis_by_review_id[review.id] for review in report_reviews]
    trust_score = _average([analysis.quality_score for analysis in report_analyses])
    viral_score = _average([analysis.viral_score for analysis in analyses])
    summary = "분석 가능한 리뷰가 아직 없습니다."
    pros: list[str] = []
    cons: list[str] = []

    if report_reviews:
        first_text = report_reviews[0].raw_text
        summary = "구체적인 구매 경험이 있는 신뢰도 높은 후기입니다."
        if "packaging" in first_text.lower() or "포장" in first_text:
            pros.append("포장 상태가 구체적으로 언급됨")
    elif reviews:
        summary = (
            "바이럴 의심 리뷰를 제외하면 리포트에 사용할 신뢰 가능한 후기가 부족합니다."
        )

    evidence_review_ids = [review.id for review in report_reviews]
    report_hash = _sha256(
        "|".join(
            [
                target_id,
                collection_run_id,
                str(trust_score),
                str(viral_score),
                *evidence_review_ids,
            ]
        )
    )

    return StoredReviewReport(
        id=str(
            uuid.uuid5(
                uuid.NAMESPACE_URL, f"{target_id}:{collection_run_id}:{report_hash}"
            )
        ),
        target_id=target_id,
        collection_run_id=collection_run_id,
        analyzer_version=ANALYZER_VERSION,
        model_provider=MODEL_PROVIDER,
        model_name=model_name,
        model_version=model_name,
        viral_contamination_score=viral_score,
        trust_score=trust_score,
        summary=summary,
        pros=pros,
        cons=cons,
        evidence_review_ids=evidence_review_ids,
        report_hash=report_hash,
    )


def _completion_event(
    *,
    correlation_id: str,
    target_id: str,
    collection_run_id: str,
    report: StoredReviewReport,
) -> dict[str, Any]:
    event_id = str(uuid.uuid4())
    return {
        "event_id": event_id,
        "event_type": "review.analysis.completed.v1",
        "schema_version": 1,
        "occurred_at": datetime.now(UTC).isoformat().replace("+00:00", "Z"),
        "correlation_id": correlation_id,
        "idempotency_key": f"review-analysis-completed:{collection_run_id}",
        "aggregate_id": target_id,
        "payload": {
            "target_id": target_id,
            "collection_run_id": collection_run_id,
            "report_id": report.id,
            "trust_score": report.trust_score,
            "viral_contamination_score": report.viral_contamination_score,
            "summary": report.summary,
        },
    }


def _normalize_text(text: str) -> str:
    return " ".join(text.casefold().split())


def _sha256(value: str) -> str:
    return hashlib.sha256(value.encode("utf-8")).hexdigest()


def _average(values: list[float]) -> float:
    if not values:
        return 0.0
    return round(sum(values) / len(values), 2)
