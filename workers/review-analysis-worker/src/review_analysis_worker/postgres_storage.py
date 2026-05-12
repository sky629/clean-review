from __future__ import annotations

import json
from typing import Protocol

from review_analysis_worker.collection_pipeline import (
    CollectionRunStatus,
    StoredReview,
    StoredReviewAnalysis,
    StoredReviewReport,
)


class DbConnection(Protocol):
    def execute(self, sql: str, params: tuple = ()):
        """Execute a SQL statement and return a cursor-like object."""


class PostgresReviewStorage:
    def __init__(self, connection: DbConnection) -> None:
        self._connection = connection

    def mark_collection_running(self, collection_run_id: str) -> None:
        self._connection.execute(
            """
            update collection_runs
               set status = %s,
                   started_at = now()
             where id = %s
            """,
            (CollectionRunStatus.RUNNING.value, collection_run_id),
        )

    def mark_collection_completed(self, collection_run_id: str) -> None:
        self._connection.execute(
            """
            update collection_runs
               set status = %s,
                   completed_at = now()
             where id = %s
            """,
            (CollectionRunStatus.COMPLETED.value, collection_run_id),
        )

    def mark_collection_failed(
        self, collection_run_id: str, failure_code: str, failure_message: str
    ) -> None:
        self._connection.execute(
            """
            update collection_runs
               set status = %s,
                   failure_code = %s,
                   failure_message = %s,
                   completed_at = now()
             where id = %s
            """,
            (
                CollectionRunStatus.FAILED.value,
                failure_code,
                failure_message,
                collection_run_id,
            ),
        )

    def save_review(self, review: StoredReview) -> StoredReview | None:
        cursor = self._connection.execute(
            """
            insert into reviews (
                id,
                target_id,
                collection_run_id,
                source,
                source_review_id,
                canonical_url,
                canonical_url_hash,
                normalized_text_hash,
                author_hash,
                raw_text,
                published_at,
                collected_at,
                status,
                created_at,
                updated_at
            )
            values (
                %s, %s, %s, %s, %s,
                %s, %s, %s, %s, %s,
                nullif(%s, '')::timestamptz,
                now(),
                %s,
                now(),
                now()
            )
            on conflict (source, canonical_url_hash)
            do nothing
            """,
            (
                review.id,
                review.target_id,
                review.collection_run_id,
                review.source,
                review.source_review_id,
                review.canonical_url,
                review.canonical_url_hash,
                review.normalized_text_hash,
                review.author_hash,
                review.raw_text,
                review.published_at,
                review.status,
            ),
        )
        return review if getattr(cursor, "rowcount", 1) == 1 else None

    def save_analysis(self, analysis: StoredReviewAnalysis) -> StoredReviewAnalysis:
        self._connection.execute(
            """
            insert into review_analysis (
                id,
                review_id,
                collection_run_id,
                analyzer_version,
                model_provider,
                model_name,
                model_version,
                viral_score,
                quality_score,
                is_suspicious,
                useful_for_report,
                summary,
                detected_patterns,
                evidence,
                status,
                analyzed_at
            )
            values (
                %s, %s, %s, %s, %s,
                %s, %s, %s, %s, %s, %s,
                %s,
                %s::jsonb,
                %s::jsonb,
                %s,
                now()
            )
            on conflict (review_id, analyzer_version, model_provider, model_name, model_version)
            do update set
                collection_run_id = excluded.collection_run_id,
                viral_score = excluded.viral_score,
                quality_score = excluded.quality_score,
                is_suspicious = excluded.is_suspicious,
                useful_for_report = excluded.useful_for_report,
                summary = excluded.summary,
                detected_patterns = excluded.detected_patterns,
                evidence = excluded.evidence,
                status = excluded.status,
                analyzed_at = now()
            """,
            (
                analysis.id,
                analysis.review_id,
                analysis.collection_run_id,
                analysis.analyzer_version,
                analysis.model_provider,
                analysis.model_name,
                analysis.model_version,
                analysis.viral_score,
                analysis.quality_score,
                analysis.is_suspicious,
                analysis.useful_for_report,
                analysis.summary,
                json.dumps(analysis.detected_patterns, ensure_ascii=False),
                json.dumps(analysis.evidence, ensure_ascii=False),
                analysis.status,
            ),
        )
        return analysis

    def save_report(self, report: StoredReviewReport) -> StoredReviewReport:
        self._connection.execute(
            """
            insert into review_reports (
                id,
                target_id,
                collection_run_id,
                analyzer_version,
                model_provider,
                model_name,
                model_version,
                viral_contamination_score,
                trust_score,
                summary,
                pros,
                cons,
                evidence_review_ids,
                report_hash,
                created_at
            )
            values (
                %s, %s, %s, %s, %s,
                %s, %s, %s, %s, %s,
                %s::jsonb,
                %s::jsonb,
                %s::jsonb,
                %s,
                now()
            )
            on conflict (target_id, collection_run_id, analyzer_version, model_version)
            do update set
                viral_contamination_score = excluded.viral_contamination_score,
                trust_score = excluded.trust_score,
                summary = excluded.summary,
                pros = excluded.pros,
                cons = excluded.cons,
                evidence_review_ids = excluded.evidence_review_ids,
                report_hash = excluded.report_hash
            """,
            (
                report.id,
                report.target_id,
                report.collection_run_id,
                report.analyzer_version,
                report.model_provider,
                report.model_name,
                report.model_version,
                report.viral_contamination_score,
                report.trust_score,
                report.summary,
                json.dumps(report.pros, ensure_ascii=False),
                json.dumps(report.cons, ensure_ascii=False),
                json.dumps(report.evidence_review_ids, ensure_ascii=False),
                report.report_hash,
            ),
        )
        return report
