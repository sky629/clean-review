from __future__ import annotations

DEFAULT_USER_AGENT = (
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
    "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0 Safari/537.36"
)


def sync_playwright():
    from playwright.sync_api import sync_playwright as playwright_sync

    return playwright_sync()
