from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class CollectedReview:
    review_id: str
    author: str
    rating: int | None
    created_at: str
    body: str
    canonical_url: str | None = None
    first_image_url: str | None = None
