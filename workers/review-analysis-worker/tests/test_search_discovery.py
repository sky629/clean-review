from datetime import datetime, timezone

from review_analysis_worker.collectors.search_discovery import (
    NaverBlogApiSearchDiscovery,
)


class FakeNaverApiClient:
    def __init__(self, pages: list[dict]) -> None:
        self.pages = pages
        self.calls = []

    def search(self, *, query: str, display: int, start: int, sort: str):
        self.calls.append(
            {"query": query, "display": display, "start": start, "sort": sort}
        )
        return self.pages.pop(0)


def test_naver_blog_api_discovery_uses_date_sort_and_window_filter() -> None:
    client = FakeNaverApiClient(
        [
            {
                "items": [
                    {
                        "link": "https://blog.naver.com/example/223",
                        "title": "<b>성수동</b> 파스타 후기",
                        "postdate": "20260510",
                    },
                    {
                        "link": "https://blog.naver.com/example/222",
                        "title": "오래된 후기",
                        "postdate": "20260401",
                    },
                ]
            }
        ]
    )
    discovery = NaverBlogApiSearchDiscovery(client=client, max_results=100)

    candidates = discovery.discover(
        keyword="성수동 파스타 맛집",
        window_from=datetime(2026, 4, 11, tzinfo=timezone.utc),
        window_to=datetime(2026, 5, 11, tzinfo=timezone.utc),
        max_results=100,
    )

    assert client.calls == [
        {
            "query": "성수동 파스타 맛집",
            "display": 100,
            "start": 1,
            "sort": "date",
        }
    ]
    assert [candidate.url for candidate in candidates] == [
        "https://blog.naver.com/example/223"
    ]
    assert candidates[0].title == "성수동 파스타 후기"
    assert candidates[0].published_at == "20260510"
