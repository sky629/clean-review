from review_analysis_worker.analyzers.gemini import (
    GeminiAnalyzer,
    ReviewAnalysisRequest,
)


class FakeGeminiClient:
    def __init__(self) -> None:
        self.calls: list[dict[str, str]] = []

    def generate_content(self, *, model: str, prompt: str) -> str:
        self.calls.append({"model": model, "prompt": prompt})
        return (
            '{"sentiment":"positive","summary":"포장이 깔끔하다는 구체적인 후기입니다.","topics":["포장"],'
            '"viral_score":12.0,"quality_score":91.0,'
            '"pros":["포장 상태 근거가 구체적임"],"cons":[],'
            '"is_suspicious":false,"useful_for_report":true,'
            '"detected_patterns":[],"evidence":["떡이 쫄깃했고 포장도 깔끔"]}'
        )


def test_gemini_analyzer_uses_flash_model_by_default() -> None:
    client = FakeGeminiClient()
    analyzer = GeminiAnalyzer(client=client)

    result = analyzer.analyze(
        ReviewAnalysisRequest(
            review_id="naver-101",
            body="떡이 쫄깃했고 포장도 깔끔해서 집에 와서도 먹기 좋았습니다.",
            rating=5,
        )
    )

    assert client.calls[0]["model"] == "gemini-2.5-flash"
    assert "떡이 쫄깃했고 포장도 깔끔" in client.calls[0]["prompt"]
    assert result.sentiment == "positive"
    assert result.summary == "포장이 깔끔하다는 구체적인 후기입니다."
    assert result.topics == ["포장"]
    assert result.viral_score == 12.0
    assert result.quality_score == 91.0
    assert result.is_suspicious is False
    assert result.useful_for_report is True
    assert result.pros == ["포장 상태 근거가 구체적임"]
    assert result.evidence == ["떡이 쫄깃했고 포장도 깔끔"]
    assert "한국어" in client.calls[0]["prompt"]
    assert "summary는 한국어 한 문장, 80자 내외" in client.calls[0]["prompt"]
    assert "현재 날짜:" in client.calls[0]["prompt"]
    assert "viral_score는 0=오염 없음, 100=명백한 바이럴" in client.calls[0]["prompt"]
    assert "is_suspicious" in client.calls[0]["prompt"]
    assert "useful_for_report" in client.calls[0]["prompt"]
    assert "#협찬" in client.calls[0]["prompt"]
    assert "이미지 라벨" in client.calls[0]["prompt"]


class InconsistentGeminiClient:
    def generate_content(self, *, model: str, prompt: str) -> str:
        return (
            '{"sentiment":"Positive","summary":"English summary","topics":["Restaurant"],'
            '"viral_score":9.0,"quality_score":1.0,'
            '"pros":[],"cons":[],'
            '"is_suspicious":true,"useful_for_report":true,'
            '"detected_patterns":["Influencer Marketing"],"evidence":["influencer"]}'
        )


def test_gemini_analyzer_normalizes_inconsistent_suspicious_scores() -> None:
    analyzer = GeminiAnalyzer(client=InconsistentGeminiClient())

    result = analyzer.analyze(
        ReviewAnalysisRequest(
            review_id="review-1",
            body="여행 인플루언서가 작성한 키워드 반복 후기입니다.",
            rating=None,
        )
    )

    assert result.is_suspicious is True
    assert result.useful_for_report is False
    assert result.viral_score == 70.0


class MarkdownWrappedGeminiClient:
    def generate_content(self, *, model: str, prompt: str) -> str:
        return (
            "```json\n"
            '{"sentiment":"neutral","summary":"핵심 경험만 짧게 요약했습니다.","topics":["가격"],'
            '"viral_score":20.0,"quality_score":70.0,'
            '"pros":["가격 언급"],"cons":[],'
            '"is_suspicious":false,"useful_for_report":true,'
            '"detected_patterns":[],"evidence":["가격 비교"]}'
            "\n```"
        )


def test_gemini_analyzer_extracts_json_from_markdown_wrapped_response() -> None:
    analyzer = GeminiAnalyzer(client=MarkdownWrappedGeminiClient())

    result = analyzer.analyze(
        ReviewAnalysisRequest(
            review_id="review-1",
            body="가격 비교를 직접 해봤습니다.",
            rating=None,
        )
    )

    assert result.summary == "핵심 경험만 짧게 요약했습니다."
    assert result.viral_score == 20.0
    assert result.quality_score == 70.0


class PaidDisclosureGeminiClient:
    def generate_content(self, *, model: str, prompt: str) -> str:
        return (
            '{"sentiment":"positive","summary":"맛과 분위기를 간략히 요약했습니다.","topics":["맛"],'
            '"viral_score":10.0,"quality_score":90.0,'
            '"pros":["맛 언급"],"cons":[],'
            '"is_suspicious":false,"useful_for_report":true,'
            '"detected_patterns":[],"evidence":[]}'
        )


def test_gemini_analyzer_forces_low_trust_when_paid_disclosure_is_in_image_label() -> (
    None
):
    analyzer = GeminiAnalyzer(client=PaidDisclosureGeminiClient())

    result = analyzer.analyze(
        ReviewAnalysisRequest(
            review_id="review-sponsored-image",
            body="파스타가 맛있었습니다.\n\n이미지 협찬/광고 라벨 신호: #협찬",
            rating=5,
        )
    )

    assert result.is_suspicious is True
    assert result.useful_for_report is False
    assert result.viral_score == 85.0
    assert result.quality_score == 40.0
    assert "대가성/협찬 표기" in result.detected_patterns
    assert "#협찬" in result.evidence[0]


class MalformedGeminiClient:
    def generate_content(self, *, model: str, prompt: str) -> str:
        return (
            '{"sentiment":"neutral","summary":"따옴표가 "깨진" 응답",'
            '"viral_score":10.0,"quality_score":90.0}'
        )


def test_gemini_analyzer_falls_back_when_response_json_is_malformed() -> None:
    analyzer = GeminiAnalyzer(client=MalformedGeminiClient())

    result = analyzer.analyze(
        ReviewAnalysisRequest(
            review_id="review-paid-malformed",
            body="#협찬\n홍대 카페를 방문해 음료와 좌석 분위기를 확인했습니다.",
            rating=None,
        )
    )

    assert (
        result.summary == "#협찬 홍대 카페를 방문해 음료와 좌석 분위기를 확인했습니다."
    )
    assert result.is_suspicious is True
    assert result.useful_for_report is False
    assert result.viral_score == 85.0
    assert result.quality_score <= 40.0
    assert "대가성/협찬 표기" in result.detected_patterns
    assert "Gemini JSON 파싱 실패" in result.detected_patterns
