from __future__ import annotations

from typing import Any

from review_analysis_worker.collectors.collected_review import CollectedReview
from review_analysis_worker.collectors.errors import CollectionBlocked
from review_analysis_worker.collectors.search_discovery import SearchResultCandidate


class GenericReviewPageExtractor:
    def __init__(self, *, timeout_millis: int = 15000) -> None:
        self._timeout_millis = timeout_millis

    def extract(
        self, *, page: Any, candidate: SearchResultCandidate
    ) -> CollectedReview | None:
        page.goto(
            candidate.url, wait_until="domcontentloaded", timeout=self._timeout_millis
        )
        page.wait_for_selector("body", timeout=self._timeout_millis)
        if hasattr(page, "wait_for_timeout"):
            page.wait_for_timeout(1000)
        raw_item = _extract_raw_item(page)
        if raw_item is None:
            return None

        canonical_url = str(raw_item.get("canonical_url") or candidate.url).strip()
        title = str(raw_item.get("title") or candidate.title).strip()
        body = _with_image_disclosure_signals(
            body=str(raw_item.get("body") or "").strip(),
            image_signals=raw_item.get("image_disclosure_signals"),
        )
        if _is_blocked_page(canonical_url=canonical_url, title=title, body=body):
            raise CollectionBlocked(f"collection blocked for {candidate.source}")
        if not body:
            return None

        return CollectedReview(
            review_id=canonical_url or candidate.url,
            author=str(raw_item.get("author") or "").strip(),
            rating=(
                int(raw_item["rating"]) if raw_item.get("rating") is not None else None
            ),
            created_at=str(
                raw_item.get("created_at") or candidate.published_at or ""
            ).strip(),
            body=body,
            canonical_url=canonical_url or candidate.url,
            first_image_url=_normalize_first_image_url(raw_item.get("first_image_url")),
        )


def _page_extraction_script() -> str:
    return """
    () => {
      const canonical = document.querySelector('link[rel="canonical"]');
      const author = document.querySelector('[rel="author"], .author, .byline, [class*="author"]');
      const published = document.querySelector('time, meta[property="article:published_time"], meta[name="date"]');
      const contentSelectors = [
        '.se-main-container',
        '#postViewArea',
        '.post_ct',
        '.post-view',
        '.blog_content',
        'article',
        'main',
        '[role="main"]',
        'body'
      ];
      const article = contentSelectors
        .map((selector) => document.querySelector(selector))
        .find((element) => element && element.textContent.trim().length > 0);
      const title = document.title || '';
      const body = article ? article.textContent.trim() : '';
      const paidDisclosurePattern = /(협찬|광고|대가성|제품\\s*제공|무상\\s*제공|원고료|체험단|서포터즈|sponsored|sponsor|gifted|paid\\s*partnership|#ad)/i;
      const imageDisclosureSignals = article
        ? Array.from(article.querySelectorAll('img'))
            .map((image) => [
              image.getAttribute('alt') || '',
              image.getAttribute('title') || '',
              image.getAttribute('aria-label') || '',
              image.currentSrc || image.src || ''
            ].join(' ').trim())
            .filter((text) => paidDisclosurePattern.test(text))
            .slice(0, 10)
        : [];
      const firstImage = article ? article.querySelector('img') : null;
      const firstImageUrl = firstImage ? (firstImage.currentSrc || firstImage.src || '') : '';
      return {
        canonical_url: canonical ? canonical.href : window.location.href,
        title,
        author: author ? author.textContent.trim() : '',
        created_at: published ? (published.getAttribute('datetime') || published.getAttribute('content') || published.textContent.trim()) : '',
        rating: null,
        body,
        image_disclosure_signals: imageDisclosureSignals,
        first_image_url: firstImageUrl
      };
    }
    """


def _with_image_disclosure_signals(*, body: str, image_signals: object) -> str:
    if not isinstance(image_signals, list):
        return body
    signals = [str(signal).strip() for signal in image_signals if str(signal).strip()]
    if not signals:
        return body
    return "\n\n".join([body, "이미지 협찬/광고 라벨 신호: " + " | ".join(signals)])


def _normalize_first_image_url(image_url: object) -> str | None:
    url = str(image_url or "").strip()
    if not url.startswith(("http://", "https://")):
        return None
    return url


def _extract_raw_item(page: Any) -> dict[str, Any] | None:
    raw_item = _evaluate_page(page)
    if _has_body(raw_item):
        return raw_item

    for frame in getattr(page, "frames", []):
        raw_frame_item = _evaluate_page(frame)
        if _has_body(raw_frame_item):
            return raw_frame_item
    return raw_item if isinstance(raw_item, dict) else None


def _evaluate_page(page: Any) -> dict[str, Any] | None:
    try:
        raw_item = page.evaluate(_page_extraction_script())
    except Exception:
        return None
    return raw_item if isinstance(raw_item, dict) else None


def _has_body(raw_item: dict[str, Any] | None) -> bool:
    return bool(raw_item and str(raw_item.get("body") or "").strip())


def _is_blocked_page(*, canonical_url: str, title: str, body: str) -> bool:
    text = " ".join([canonical_url, title, body]).casefold()
    blocked_patterns = [
        "unusual traffic",
        "captcha",
        "robot",
        "로봇이 아닙니다",
        "자동입력",
        "비정상적인 트래픽",
    ]
    return any(pattern in text for pattern in blocked_patterns)
