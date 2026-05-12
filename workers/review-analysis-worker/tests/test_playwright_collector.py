import pytest

from review_analysis_worker.collectors.playwright import PlaywrightReviewCollector
from review_analysis_worker.collectors.review_source import UnsupportedReviewSource


class FakeSourceCollector:
    def __init__(self) -> None:
        self.calls: list[dict[str, str]] = []

    def collect(self, *, source: str, keyword: str, **kwargs) -> list[str]:
        self.calls.append({"source": source, "keyword": keyword})
        return ["review-1"]


def test_playwright_collector_routes_by_review_source() -> None:
    naver_collector = FakeSourceCollector()
    collector = PlaywrightReviewCollector(
        source_collectors={
            "NAVER_BLOG": naver_collector,
        }
    )

    naver_reviews = collector.collect(source="NAVER_BLOG", keyword="clean review")

    assert naver_reviews == ["review-1"]
    assert naver_collector.calls == [
        {"source": "NAVER_BLOG", "keyword": "clean review"}
    ]


def test_playwright_collector_rejects_unsupported_source() -> None:
    collector = PlaywrightReviewCollector(source_collectors={})

    with pytest.raises(UnsupportedReviewSource):
        collector.collect(source="INSTAGRAM", keyword="clean review")
