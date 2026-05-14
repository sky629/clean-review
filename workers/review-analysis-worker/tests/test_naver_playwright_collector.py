from review_analysis_worker.collectors.naver_playwright import (
    NaverBlogPlaywrightCollector,
)
from review_analysis_worker.collectors.search_discovery import SearchResultCandidate


class FakePage:
    def __init__(self, raw_result) -> None:
        self.raw_result = raw_result
        self.goto_url = ""
        self.waited_for = ""
        self.closed = False

    def goto(self, url: str, wait_until: str, timeout: int) -> None:
        self.goto_url = url
        self.wait_until = wait_until
        self.timeout = timeout

    def wait_for_selector(self, selector: str, timeout: int) -> None:
        self.waited_for = selector
        self.selector_timeout = timeout

    def evaluate(self, script: str):
        self.script = script
        return self.raw_result

    def close(self) -> None:
        self.closed = True


class FakeBrowser:
    def __init__(self, pages: list[FakePage]) -> None:
        self.pages = pages
        self.created_pages: list[FakePage] = []
        self.closed = False

    def new_page(self, user_agent: str, locale: str):
        self.user_agent = user_agent
        self.locale = locale
        page = self.pages.pop(0)
        self.created_pages.append(page)
        return page

    def close(self) -> None:
        self.closed = True


class FakeChromium:
    def __init__(self, browser: FakeBrowser) -> None:
        self.browser = browser

    def launch(self, headless: bool):
        self.headless = headless
        return self.browser


class FakePlaywright:
    def __init__(self, pages: list[FakePage]) -> None:
        self.browser = FakeBrowser(pages)
        self.chromium = FakeChromium(self.browser)

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc, traceback):
        return False


class FakeDiscovery:
    def __init__(self) -> None:
        self.calls = []

    def discover(
        self,
        *,
        page,
        keyword: str,
        window_from=None,
        window_to=None,
        max_results: int | None = None,
    ):
        self.calls.append(
            {
                "page": page,
                "keyword": keyword,
                "max_results": max_results,
            }
        )
        return [
            SearchResultCandidate(
                source="NAVER_BLOG",
                url="https://blog.naver.com/example/223",
                title="성수동 파스타 후기",
            )
        ]


def test_naver_blog_playwright_collector_collects_real_search_results() -> None:
    search_page = FakePage([])
    review_page = FakePage(
        {
            "canonical_url": "https://blog.naver.com/example/223",
            "title": "성수동 파스타 후기",
            "author": "summer",
            "created_at": "2026-05-08",
            "body": "웨이팅은 20분 정도였고 소스가 진해서 좋았습니다.",
        }
    )
    discovery = FakeDiscovery()
    collector = NaverBlogPlaywrightCollector(
        discovery=discovery,
        playwright_factory=lambda: FakePlaywright([search_page, review_page]),
        max_reviews=3,
        timeout_millis=5000,
    )

    reviews = collector.collect(source="NAVER_BLOG", keyword="성수동 파스타 맛집")

    assert discovery.calls == [
        {"page": search_page, "keyword": "성수동 파스타 맛집", "max_results": 3}
    ]
    assert review_page.goto_url == "https://blog.naver.com/example/223"
    assert review_page.waited_for == "body"
    assert len(reviews) == 1
    assert reviews[0].review_id == "https://blog.naver.com/example/223"
    assert reviews[0].canonical_url == "https://blog.naver.com/example/223"
    assert reviews[0].body == "웨이팅은 20분 정도였고 소스가 진해서 좋았습니다."
    assert review_page.script.count("querySelector") > 0
    assert review_page.closed is True
