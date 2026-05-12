from __future__ import annotations


class GeminiRuntimeDependencyError(RuntimeError):
    pass


class GoogleGenaiGeminiClient:
    def __init__(self, api_key: str) -> None:
        try:
            from google import genai
        except ImportError as exc:
            raise GeminiRuntimeDependencyError(
                "google-genai is required to run Gemini analysis."
            ) from exc

        self._client = genai.Client(api_key=api_key)

    def generate_content(self, *, model: str, prompt: str) -> str:
        response = self._client.models.generate_content(model=model, contents=prompt)
        text = getattr(response, "text", None)
        if not isinstance(text, str) or not text.strip():
            raise RuntimeError("Gemini response did not include text.")
        return text

    def generate_content_with_image(
        self,
        *,
        model: str,
        prompt: str,
        image_bytes: bytes,
        mime_type: str,
    ) -> str:
        try:
            from google.genai import types
        except ImportError as exc:
            raise GeminiRuntimeDependencyError(
                "google-genai is required to run Gemini image OCR."
            ) from exc

        response = self._client.models.generate_content(
            model=model,
            contents=[
                types.Part.from_text(text=prompt),
                types.Part.from_bytes(data=image_bytes, mime_type=mime_type),
            ],
        )
        text = getattr(response, "text", None)
        return text.strip() if isinstance(text, str) else ""
