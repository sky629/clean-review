import pytest

from review_analysis_worker.collectors.errors import CollectionBlocked
from review_analysis_worker.collectors.page_extractor import GenericReviewPageExtractor
from review_analysis_worker.collectors.search_discovery import SearchResultCandidate


class FakeReviewPage:
    def __init__(self, raw_item: dict, frames: list | None = None) -> None:
        self.raw_item = raw_item
        self.frames = frames or []
        self.goto_url = ""
        self.waited_for = ""

    def goto(self, url: str, wait_until: str, timeout: int) -> None:
        self.goto_url = url
        self.wait_until = wait_until
        self.timeout = timeout

    def wait_for_selector(self, selector: str, timeout: int) -> None:
        self.waited_for = selector
        self.selector_timeout = timeout

    def wait_for_timeout(self, timeout: int) -> None:
        self.waited_timeout = timeout

    def evaluate(self, script: str):
        self.script = script
        return self.raw_item


def test_generic_review_page_extractor_builds_collected_review_from_page_body() -> None:
    page = FakeReviewPage(
        {
            "canonical_url": "https://example.com/product-review",
            "title": "무선 이어폰 실사용 후기",
            "author": "real-user",
            "created_at": "2026-05-08",
            "body": "3주 동안 사용했고 배터리는 하루 정도 충분했습니다.",
        }
    )
    extractor = GenericReviewPageExtractor(timeout_millis=5000)
    candidate = SearchResultCandidate(
        source="NAVER_BLOG",
        url="https://example.com/product-review",
        title="무선 이어폰 실사용 후기",
    )

    review = extractor.extract(page=page, candidate=candidate)

    assert page.goto_url == "https://example.com/product-review"
    assert page.waited_for == "body"
    assert page.waited_timeout == 1000
    assert review is not None
    assert review.review_id == "https://example.com/product-review"
    assert review.canonical_url == "https://example.com/product-review"
    assert review.author == "real-user"
    assert review.created_at == "2026-05-08"
    assert review.body == "3주 동안 사용했고 배터리는 하루 정도 충분했습니다."
    assert page.script.count("querySelector") > 0


def test_generic_review_page_extractor_appends_paid_image_label_signals() -> None:
    page = FakeReviewPage(
        {
            "canonical_url": "https://example.com/sponsored-review",
            "title": "연남동 파스타 후기",
            "author": "real-user",
            "created_at": "2026-05-08",
            "body": "파스타 맛과 웨이팅 경험을 남긴 후기입니다.",
            "image_disclosure_signals": ["#협찬", "sponsored badge"],
            "first_image_url": "https://example.com/first-sponsored-image.png",
        }
    )
    extractor = GenericReviewPageExtractor(timeout_millis=5000)
    candidate = SearchResultCandidate(
        source="NAVER_BLOG",
        url="https://example.com/sponsored-review",
        title="연남동 파스타 후기",
    )

    review = extractor.extract(page=page, candidate=candidate)

    assert review is not None
    assert "파스타 맛과 웨이팅 경험" in review.body
    assert "이미지 협찬/광고 라벨 신호: #협찬 | sponsored badge" in review.body
    assert review.first_image_url == "https://example.com/first-sponsored-image.png"


def test_generic_review_page_extractor_ignores_empty_body() -> None:
    page = FakeReviewPage(
        {
            "canonical_url": "https://example.com/empty",
            "title": "빈 페이지",
            "author": "",
            "created_at": "",
            "body": "",
        }
    )
    extractor = GenericReviewPageExtractor()
    candidate = SearchResultCandidate(
        source="NAVER_BLOG", url="https://example.com/empty", title="빈 페이지"
    )

    assert extractor.extract(page=page, candidate=candidate) is None


def test_generic_review_page_extractor_reads_same_origin_frame_when_top_body_is_empty() -> (
    None
):
    frame = FakeReviewPage(
        {
            "canonical_url": "https://blog.naver.com/example/223",
            "title": "성수동 파스타 후기",
            "author": "summer",
            "created_at": "2026-05-08",
            "body": "iframe 안에 있는 네이버 블로그 본문입니다.",
        }
    )
    page = FakeReviewPage(
        {
            "canonical_url": "https://blog.naver.com/example/223",
            "title": "성수동 파스타 후기",
            "author": "",
            "created_at": "",
            "body": "",
        },
        frames=[frame],
    )
    extractor = GenericReviewPageExtractor()
    candidate = SearchResultCandidate(
        source="NAVER_BLOG",
        url="https://blog.naver.com/example/223",
        title="성수동 파스타 후기",
    )

    review = extractor.extract(page=page, candidate=candidate)

    assert review is not None
    assert review.body == "iframe 안에 있는 네이버 블로그 본문입니다."
    assert review.author == "summer"


def test_generic_review_page_extractor_detects_blocked_page() -> None:
    page = FakeReviewPage(
        {
            "canonical_url": "https://blog.naver.com/blocked/223",
            "title": "unusual traffic",
            "author": "",
            "created_at": "",
            "body": "비정상적인 트래픽이 감지되었습니다.",
        }
    )
    extractor = GenericReviewPageExtractor()
    candidate = SearchResultCandidate(
        source="NAVER_BLOG", url="https://example.com/review", title="review"
    )

    with pytest.raises(CollectionBlocked):
        extractor.extract(page=page, candidate=candidate)
