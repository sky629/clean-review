from __future__ import annotations

from typing import Any, Protocol

from review_analysis_worker.collectors.review_source import UnsupportedReviewSource


class SourceCollector(Protocol):
    def collect(
        self,
        *,
        source: str,
        keyword: str,
        window_from=None,
        window_to=None,
        max_reviews: int | None = None,
    ) -> list[Any]:
        """Collect reviews for one concrete source."""


class PlaywrightReviewCollector:
    def __init__(
        self,
        *,
        source_collectors: dict[str, SourceCollector],
    ) -> None:
        self._source_collectors = source_collectors

    def collect(
        self,
        *,
        source: str,
        keyword: str,
        window_from=None,
        window_to=None,
        max_reviews: int | None = None,
    ) -> list[Any]:
        collector = self._source_collectors.get(source)
        if collector is None:
            raise UnsupportedReviewSource(source)
        return collector.collect(
            source=source,
            keyword=keyword,
            window_from=window_from,
            window_to=window_to,
            max_reviews=max_reviews,
        )
