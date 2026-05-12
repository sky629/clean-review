from review_analysis_worker.runtime import first_image_ocr
from review_analysis_worker.runtime.first_image_ocr import GeminiFirstImageOcr


class FakeGeminiClient:
    def __init__(self) -> None:
        self.calls = []

    def generate_content_with_image(
        self, *, model: str, prompt: str, image_bytes: bytes, mime_type: str
    ) -> str:
        self.calls.append(
            {
                "model": model,
                "prompt": prompt,
                "image_bytes": image_bytes,
                "mime_type": mime_type,
            }
        )
        return "#협찬"


def test_gemini_first_image_ocr_extracts_text_from_one_image(monkeypatch) -> None:
    monkeypatch.setattr(
        first_image_ocr,
        "_fetch_image",
        lambda image_url: (b"first-image", "image/png"),
    )
    client = FakeGeminiClient()
    ocr = GeminiFirstImageOcr(client=client, model="gemini-2.5-flash")

    result = ocr.extract_text("https://example.com/first.png")

    assert result == "#협찬"
    assert len(client.calls) == 1
    assert client.calls[0]["model"] == "gemini-2.5-flash"
    assert client.calls[0]["image_bytes"] == b"first-image"
    assert client.calls[0]["mime_type"] == "image/png"
    assert "협찬" in client.calls[0]["prompt"]


def test_gemini_first_image_ocr_skips_missing_or_unreadable_image(monkeypatch) -> None:
    monkeypatch.setattr(first_image_ocr, "_fetch_image", lambda image_url: None)
    client = FakeGeminiClient()
    ocr = GeminiFirstImageOcr(client=client, model="gemini-2.5-flash")

    assert ocr.extract_text(None) == ""
    assert ocr.extract_text("https://example.com/broken.png") == ""
    assert client.calls == []
