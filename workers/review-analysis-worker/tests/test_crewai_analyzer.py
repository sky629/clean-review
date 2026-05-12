import sys
from types import SimpleNamespace

from review_analysis_worker.analyzers.crewai import CrewAIReviewAnalyzer
from review_analysis_worker.analyzers.gemini import ReviewAnalysisRequest


class FakeCrewRunner:
    def __init__(self) -> None:
        self.calls = []

    def run(self, *, review_id: str, body: str, rating: int | None) -> dict:
        self.calls.append({"review_id": review_id, "body": body, "rating": rating})
        return {
            "sentiment": "positive",
            "summary": "웨이팅과 포장 상태가 구체적으로 언급된 실사용 후기입니다.",
            "topics": ["waiting", "packaging"],
            "viral_score": 8.0,
            "quality_score": 88.0,
            "is_suspicious": False,
            "useful_for_report": True,
            "pros": ["포장 상태가 좋음"],
            "cons": ["웨이팅 20분"],
            "detected_patterns": [],
            "evidence": ["웨이팅은 20분", "포장 상태가 좋았습니다"],
        }


def test_crewai_review_analyzer_uses_two_agent_runner_contract() -> None:
    runner = FakeCrewRunner()
    analyzer = CrewAIReviewAnalyzer(runner=runner, model="gemini-2.5-flash")

    result = analyzer.analyze(
        ReviewAnalysisRequest(
            review_id="review-1",
            body="웨이팅은 20분이었고 포장 상태가 좋았습니다.",
            rating=5,
        )
    )

    assert analyzer.model == "gemini-2.5-flash"
    assert runner.calls == [
        {
            "review_id": "review-1",
            "body": "웨이팅은 20분이었고 포장 상태가 좋았습니다.",
            "rating": 5,
        }
    ]
    assert result.viral_score == 8.0
    assert result.quality_score == 88.0
    assert result.is_suspicious is False
    assert result.useful_for_report is True
    assert result.evidence == ["웨이팅은 20분", "포장 상태가 좋았습니다"]


class InconsistentCrewRunner:
    def run(self, *, review_id: str, body: str, rating: int | None) -> dict:
        return {
            "sentiment": "Positive",
            "summary": "English summary",
            "topics": ["Restaurant"],
            "viral_score": 9.0,
            "quality_score": 1.0,
            "is_suspicious": True,
            "useful_for_report": True,
            "pros": [],
            "cons": [],
            "detected_patterns": ["Influencer Marketing"],
            "evidence": ["influencer"],
        }


def test_crewai_review_analyzer_normalizes_inconsistent_suspicious_scores() -> None:
    analyzer = CrewAIReviewAnalyzer(
        runner=InconsistentCrewRunner(), model="gemini-2.5-flash"
    )

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


def test_crewai_review_analyzer_forces_low_trust_when_paid_disclosure_is_in_image_label() -> (
    None
):
    analyzer = CrewAIReviewAnalyzer(runner=FakeCrewRunner(), model="gemini-2.5-flash")

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


def test_crewai_runner_prompt_locks_current_date_korean_output_and_score_scale(
    monkeypatch,
) -> None:
    captured_tasks = []

    class FakeLLM:
        def __init__(self, **kwargs):
            self.kwargs = kwargs

    class FakeAgent:
        def __init__(self, **kwargs):
            self.kwargs = kwargs

    class FakeTask:
        def __init__(self, **kwargs):
            self.kwargs = kwargs
            captured_tasks.append(kwargs)

    class FakeCrew:
        def __init__(self, **kwargs):
            self.kwargs = kwargs

        def kickoff(self):
            return SimpleNamespace(
                raw=(
                    '{"sentiment":"긍정","summary":"한국어 요약","topics":["웨이팅"],'
                    '"viral_score":12,"quality_score":88,"is_suspicious":false,'
                    '"useful_for_report":true,"pros":[],"cons":[],'
                    '"detected_patterns":[],"evidence":[]}'
                )
            )

    fake_crewai = SimpleNamespace(
        Agent=FakeAgent,
        Crew=FakeCrew,
        LLM=FakeLLM,
        Process=SimpleNamespace(sequential="sequential"),
        Task=FakeTask,
    )
    monkeypatch.setitem(sys.modules, "crewai", fake_crewai)

    from review_analysis_worker.analyzers.crewai import CrewAIReviewCrewRunner

    runner = CrewAIReviewCrewRunner(
        model="gemini-2.5-flash",
        api_key="test-key",
        current_date="2026-05-11",
    )

    runner.run(
        review_id="review-1", body="방문일 : 2026.04.07 후기입니다.", rating=None
    )

    prompt_text = "\n".join(
        task["description"] + "\n" + task["expected_output"] for task in captured_tasks
    )
    assert "현재 날짜: 2026-05-11" in prompt_text
    assert "2026-05-11 기준" in prompt_text
    assert "모든 문자열 값은 한국어로 작성" in prompt_text
    assert "summary는 한국어 한 문장, 80자 내외" in prompt_text
    assert "viral_score는 0=오염 없음, 100=명백한 바이럴" in prompt_text
    assert "is_suspicious=true이면 viral_score는 반드시 70 이상" in prompt_text
    assert "#협찬" in prompt_text
    assert "이미지 라벨" in prompt_text
