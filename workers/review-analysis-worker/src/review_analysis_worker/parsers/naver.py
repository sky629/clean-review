from __future__ import annotations

from html.parser import HTMLParser

from review_analysis_worker.collectors.collected_review import CollectedReview


def parse_naver_reviews(html: str) -> list[CollectedReview]:
    parser = _NaverBlogParser()
    parser.feed(html)
    return parser.reviews


class _NaverBlogParser(HTMLParser):
    def __init__(self) -> None:
        super().__init__()
        self.reviews: list[CollectedReview] = []
        self._current: dict[str, str] | None = None
        self._capture_field: str | None = None

    def handle_starttag(
        self,
        tag: str,
        attrs: list[tuple[str, str | None]],
    ) -> None:
        attr_map = {key: value or "" for key, value in attrs}
        classes = set(attr_map.get("class", "").split())

        if tag == "article" and "naver-review" in classes:
            self._current = {"review_id": attr_map.get("data-review-id", "")}
            return

        if self._current is None:
            return

        if tag == "time":
            self._current["created_at"] = attr_map.get("datetime", "")
        elif tag in {"span", "p"}:
            if "author" in classes:
                self._capture_field = "author"
            elif "rating" in classes:
                self._capture_field = "rating"
            elif "body" in classes:
                self._capture_field = "body"

    def handle_data(self, data: str) -> None:
        if self._current is None or self._capture_field is None:
            return
        self._current[self._capture_field] = data.strip()

    def handle_endtag(self, tag: str) -> None:
        if self._current is not None and tag == "article":
            self.reviews.append(
                CollectedReview(
                    review_id=self._current.get("review_id", ""),
                    author=self._current.get("author", ""),
                    rating=int(self._current.get("rating", "0")),
                    created_at=self._current.get("created_at", ""),
                    body=self._current.get("body", ""),
                )
            )
            self._current = None
            self._capture_field = None
            return

        if tag in {"span", "p"}:
            self._capture_field = None
