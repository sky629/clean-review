from __future__ import annotations

import html
import json
import re
from dataclasses import dataclass
from datetime import datetime
from typing import Any, Protocol
from urllib.parse import urlencode
from urllib.request import Request, urlopen

NAVER_BLOG_SEARCH_API_URL = "https://openapi.naver.com/v1/search/blog.json"
_HTML_TAG = re.compile(r"<[^>]+>")


@dataclass(frozen=True, slots=True)
class SearchResultCandidate:
    source: str
    url: str
    title: str
    published_at: str = ""


class SearchResultDiscovery(Protocol):
    def discover(
        self,
        *,
        page: Any = None,
        keyword: str,
        window_from: datetime | None = None,
        window_to: datetime | None = None,
        max_results: int | None = None,
    ) -> list[SearchResultCandidate]:
        """Discover candidate review page URLs from a search result page."""


class NaverBlogSearchApiClient:
    def __init__(
        self,
        *,
        client_id: str,
        client_secret: str,
        opener=urlopen,
    ) -> None:
        self._client_id = client_id
        self._client_secret = client_secret
        self._opener = opener

    def search(
        self, *, query: str, display: int, start: int, sort: str
    ) -> dict[str, Any]:
        if not self._client_id or not self._client_secret:
            raise ValueError("Naver search API credentials are required")
        url = f"{NAVER_BLOG_SEARCH_API_URL}?{urlencode({'query': query, 'display': display, 'start': start, 'sort': sort})}"
        request = Request(
            url,
            headers={
                "X-Naver-Client-Id": self._client_id,
                "X-Naver-Client-Secret": self._client_secret,
            },
        )
        with self._opener(request, timeout=10) as response:
            return json.loads(response.read().decode("utf-8"))


class NaverBlogApiSearchDiscovery:
    def __init__(
        self,
        *,
        client: Any,
        max_results: int = 100,
    ) -> None:
        self._client = client
        self._max_results = max_results

    def discover(
        self,
        *,
        page: Any = None,
        keyword: str,
        window_from: datetime | None = None,
        window_to: datetime | None = None,
        max_results: int | None = None,
    ) -> list[SearchResultCandidate]:
        if not keyword.strip():
            raise ValueError("keyword is required")

        limit = max_results or self._max_results
        candidates: list[SearchResultCandidate] = []
        start = 1
        while len(candidates) < limit:
            display = min(100, limit - len(candidates))
            response = self._client.search(
                query=keyword,
                display=display,
                start=start,
                sort="date",
            )
            items = response.get("items") if isinstance(response, dict) else None
            if not isinstance(items, list) or not items:
                break

            should_stop = False
            for item in items:
                if not isinstance(item, dict):
                    continue
                postdate = str(item.get("postdate") or "").strip()
                post_day = _parse_naver_postdate(postdate)
                if (
                    post_day is not None
                    and window_from is not None
                    and post_day < window_from.date()
                ):
                    should_stop = True
                    break
                if (
                    post_day is not None
                    and window_to is not None
                    and post_day > window_to.date()
                ):
                    continue
                link = html.unescape(str(item.get("link") or "")).strip()
                if not link.startswith("http"):
                    continue
                title = _clean_html(str(item.get("title") or ""))
                candidates.append(
                    SearchResultCandidate(
                        source="NAVER_BLOG",
                        url=link,
                        title=title,
                        published_at=postdate,
                    )
                )
                if len(candidates) >= limit:
                    break
            if should_stop or len(items) < display:
                break
            start += display

        return _dedupe_candidates(candidates)[:limit]


def _dedupe_candidates(candidates) -> list[SearchResultCandidate]:
    seen: set[str] = set()
    deduped: list[SearchResultCandidate] = []
    for candidate in candidates:
        if candidate.url in seen:
            continue
        seen.add(candidate.url)
        deduped.append(candidate)
    return deduped


def _parse_naver_postdate(value: str):
    if not value:
        return None
    return datetime.strptime(value, "%Y%m%d").date()


def _clean_html(value: str) -> str:
    return html.unescape(_HTML_TAG.sub("", value)).strip()
