from review_analysis_worker.collection_pipeline import (
    CollectionRunStatus,
    StoredReview,
    StoredReviewAnalysis,
    StoredReviewReport,
)
from review_analysis_worker.postgres_storage import PostgresReviewStorage


class FakeConnection:
    def __init__(self) -> None:
        self.calls: list[tuple[str, tuple]] = []
        self.rowcount = 1

    def execute(self, sql: str, params: tuple = ()):
        self.calls.append((" ".join(sql.split()), params))
        return self

    def fetchone(self):
        return None


def test_postgres_storage_updates_collection_status() -> None:
    connection = FakeConnection()
    storage = PostgresReviewStorage(connection)

    storage.mark_collection_running("run-1")
    storage.mark_collection_completed("run-1")
    storage.mark_collection_failed("run-1", "CollectionBlocked", "captcha")

    assert (
        "update collection_runs set status = %s, started_at = now()"
        in connection.calls[0][0]
    )
    assert connection.calls[0][1] == (CollectionRunStatus.RUNNING.value, "run-1")
    assert (
        "update collection_runs set status = %s, completed_at = now()"
        in connection.calls[1][0]
    )
    assert connection.calls[1][1] == (CollectionRunStatus.COMPLETED.value, "run-1")
    assert "failure_code = %s" in connection.calls[2][0]
    assert connection.calls[2][1] == (
        CollectionRunStatus.FAILED.value,
        "CollectionBlocked",
        "captcha",
        "run-1",
    )


def test_postgres_storage_upserts_review_analysis_and_report() -> None:
    connection = FakeConnection()
    storage = PostgresReviewStorage(connection)
    review = StoredReview(
        id="review-1",
        target_id="target-1",
        collection_run_id="run-1",
        source="NAVER_BLOG",
        source_review_id="naver-101",
        canonical_url="https://blog.naver.com/reviews/naver-101",
        canonical_url_hash="url-hash",
        normalized_text_hash="text-hash",
        author_hash="author-hash",
        raw_text="떡이 쫄깃했고 포장도 깔끔해서 집에 와서도 먹기 좋았습니다.",
        published_at="2026-05-06",
    )
    analysis = StoredReviewAnalysis(
        id="analysis-1",
        review_id="review-1",
        collection_run_id="run-1",
        analyzer_version="review-analysis-worker-0.1.0",
        model_provider="google",
        model_name="gemini-2.5-flash",
        model_version="gemini-2.5-flash",
        viral_score=12.0,
        quality_score=91.0,
        is_suspicious=False,
        useful_for_report=True,
        summary="포장이 깔끔하고 식감이 좋았다는 실사용 후기입니다.",
        detected_patterns=[],
        evidence=["떡이 쫄깃했고 포장도 깔끔"],
    )
    report = StoredReviewReport(
        id="report-1",
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
        evidence_review_ids=["review-1"],
        report_hash="report-hash",
    )

    assert storage.save_review(review) == review
    assert storage.save_analysis(analysis) == analysis
    assert storage.save_report(report) == report

    executed = "\n".join(call[0] for call in connection.calls)
    assert "insert into reviews" in executed
    assert "on conflict (source, canonical_url_hash)" in executed
    assert "insert into review_analysis" in executed
    assert "is_suspicious" in executed
    assert "useful_for_report" in executed
    assert "summary" in executed
    assert (
        "on conflict (review_id, analyzer_version, model_provider, model_name, model_version)"
        in executed
    )
    assert analysis.summary in connection.calls[1][1]
    assert "insert into review_reports" in executed
    assert (
        "on conflict (target_id, collection_run_id, analyzer_version, model_version)"
        in executed
    )


def test_postgres_storage_returns_none_for_duplicate_review() -> None:
    connection = FakeConnection()
    connection.rowcount = 0
    storage = PostgresReviewStorage(connection)
    review = StoredReview(
        id="review-1",
        target_id="target-1",
        collection_run_id="run-1",
        source="NAVER_BLOG",
        source_review_id="naver-101",
        canonical_url="https://blog.naver.com/reviews/naver-101",
        canonical_url_hash="url-hash",
        normalized_text_hash="text-hash",
        author_hash="author-hash",
        raw_text="떡이 쫄깃했습니다.",
        published_at="2026-05-06",
    )

    assert storage.save_review(review) is None
    assert "do nothing" in connection.calls[0][0]
