from __future__ import annotations

from collections.abc import Callable
from typing import Any

from review_analysis_worker.collectors.collected_review import CollectedReview
from review_analysis_worker.collectors.page_extractor import GenericReviewPageExtractor
from review_analysis_worker.collectors.review_source import UnsupportedReviewSource
from review_analysis_worker.collectors.search_discovery import SearchResultDiscovery
from review_analysis_worker.collectors.web import DEFAULT_USER_AGENT, sync_playwright


class NaverBlogPlaywrightCollector:
    def __init__(
        self,
        *,
        discovery: SearchResultDiscovery,
        playwright_factory: Callable[[], Any] | None = None,
        extractor: GenericReviewPageExtractor | None = None,
        max_reviews: int = 20,
        timeout_millis: int = 15000,
        headless: bool = True,
    ) -> None:
        self._playwright_factory = playwright_factory or sync_playwright
        self._discovery = discovery
        self._extractor = extractor or GenericReviewPageExtractor(
            timeout_millis=timeout_millis
        )
        self._max_reviews = max_reviews
        self._headless = headless

    def collect(
        self,
        *,
        source: str,
        keyword: str,
        window_from=None,
        window_to=None,
        max_reviews: int | None = None,
    ) -> list[CollectedReview]:
        if source != "NAVER_BLOG":
            raise UnsupportedReviewSource(source)
        if not keyword.strip():
            raise ValueError("keyword is required")

        with self._playwright_factory() as playwright:
            browser = playwright.chromium.launch(headless=self._headless)
            try:
                search_page = browser.new_page(
                    user_agent=DEFAULT_USER_AGENT, locale="ko-KR"
                )
                limit = max_reviews or self._max_reviews
                candidates = self._discovery.discover(
                    page=search_page,
                    keyword=keyword,
                    window_from=window_from,
                    window_to=window_to,
                    max_results=limit,
                )
                reviews: list[CollectedReview] = []
                for candidate in candidates:
                    review_page = browser.new_page(
                        user_agent=DEFAULT_USER_AGENT, locale="ko-KR"
                    )
                    try:
                        review = self._extractor.extract(
                            page=review_page, candidate=candidate
                        )
                    finally:
                        review_page.close()
                    if review is None:
                        continue
                    reviews.append(review)
                    if len(reviews) >= limit:
                        break
                return reviews
            finally:
                browser.close()
