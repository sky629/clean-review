from __future__ import annotations

from urllib.request import Request, urlopen

MAX_FIRST_IMAGE_BYTES = 5 * 1024 * 1024
IMAGE_FETCH_TIMEOUT_SECONDS = 8
OCR_PROMPT = (
    "이 이미지는 리뷰 글의 첫 번째 이미지입니다. "
    "#협찬, 협찬, 광고, 대가성, 제품 제공, 무상 제공, 원고료, 체험단, 서포터즈, "
    "sponsored, gifted, paid partnership 같은 대가성/협찬 표기가 보이면 보이는 문구만 그대로 반환하세요. "
    "해당 표기가 없으면 빈 문자열만 반환하세요. 설명, 마크다운, JSON은 쓰지 마세요."
)


class GeminiFirstImageOcr:
    def __init__(self, *, client, model: str) -> None:
        self._client = client
        self._model = model

    def extract_text(self, image_url: str | None) -> str:
        if not image_url:
            return ""
        image = _fetch_image(image_url)
        if image is None:
            return ""
        image_bytes, mime_type = image
        return self._client.generate_content_with_image(
            model=self._model,
            prompt=OCR_PROMPT,
            image_bytes=image_bytes,
            mime_type=mime_type,
        ).strip()


def _fetch_image(image_url: str) -> tuple[bytes, str] | None:
    try:
        request = Request(
            image_url,
            headers={
                "User-Agent": "Mozilla/5.0 (compatible; CleanReviewBot/0.1)",
                "Accept": "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8",
            },
        )
        with urlopen(request, timeout=IMAGE_FETCH_TIMEOUT_SECONDS) as response:
            content_type = response.headers.get_content_type()
            if not content_type.startswith("image/"):
                return None
            image_bytes = response.read(MAX_FIRST_IMAGE_BYTES + 1)
            if len(image_bytes) > MAX_FIRST_IMAGE_BYTES:
                return None
            return image_bytes, content_type
    except Exception:
        return None
