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

    def mark_collection_running(self, collection_run_id: str) -> bool:
        cursor = self._connection.execute(
            """
            update collection_runs
               set status = %s,
                   started_at = now(),
                   failure_code = null,
                   failure_message = null
             where id = %s
               and status in ('REQUESTED', 'FAILED')
            """,
            (CollectionRunStatus.RUNNING.value, collection_run_id),
        )
        return getattr(cursor, "rowcount", 0) == 1

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

    def find_review_by_source_and_url_hash(
        self, *, source: str, canonical_url_hash: str
    ) -> StoredReview | None:
        cursor = self._connection.execute(
            """
            select id,
                   target_id,
                   collection_run_id,
                   source,
                   source_review_id,
                   canonical_url,
                   canonical_url_hash,
                   normalized_text_hash,
                   author_hash,
                   raw_text,
                   coalesce(published_at::text, ''),
                   status
              from reviews
             where source = %s
               and canonical_url_hash = %s
             limit 1
            """,
            (source, canonical_url_hash),
        )
        row = cursor.fetchone()
        if row is None:
            return None
        return StoredReview(
            id=str(row[0]),
            target_id=str(row[1]),
            collection_run_id=str(row[2] or ""),
            source=str(row[3]),
            source_review_id=str(row[4] or ""),
            canonical_url=str(row[5]),
            canonical_url_hash=str(row[6]),
            normalized_text_hash=str(row[7]),
            author_hash=str(row[8] or ""),
            raw_text=str(row[9]),
            published_at=str(row[10] or ""),
            status=str(row[11]),
        )

    def analysis_exists(
        self,
        *,
        review_id: str,
        analyzer_version: str,
        model_provider: str,
        model_name: str,
        model_version: str,
    ) -> bool:
        cursor = self._connection.execute(
            """
            select 1
              from review_analysis
             where review_id = %s
               and analyzer_version = %s
               and model_provider = %s
               and model_name = %s
               and model_version = %s
             limit 1
            """,
            (review_id, analyzer_version, model_provider, model_name, model_version),
        )
        return cursor.fetchone() is not None

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
              do nothing
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
