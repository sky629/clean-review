from __future__ import annotations

import json
from datetime import datetime
from typing import Any, Protocol
from zoneinfo import ZoneInfo

from review_analysis_worker.analyzers.gemini import (
    DEFAULT_GEMINI_MODEL,
    ReviewAnalysisRequest,
    ReviewAnalysisResult,
    enforce_paid_disclosure_policy,
)


class CrewAIRuntimeDependencyError(RuntimeError):
    pass


SUSPICIOUS_VIRAL_SCORE_FLOOR = 70.0


class ReviewCrewRunner(Protocol):
    def run(self, *, review_id: str, body: str, rating: int | None) -> dict[str, Any]:
        """Run the two-agent review analysis crew and return normalized JSON."""


class CrewAIReviewAnalyzer:
    def __init__(
        self,
        runner: ReviewCrewRunner,
        model: str = DEFAULT_GEMINI_MODEL,
    ) -> None:
        self._runner = runner
        self._model = model

    @property
    def model(self) -> str:
        return self._model

    def analyze(self, request: ReviewAnalysisRequest) -> ReviewAnalysisResult:
        parsed = self._runner.run(
            review_id=request.review_id,
            body=request.body,
            rating=request.rating,
        )
        viral_score = _clamp_score(float(parsed.get("viral_score", 0.0)))
        quality_score = _clamp_score(float(parsed.get("quality_score", 0.0)))
        is_suspicious = bool(parsed.get("is_suspicious", False))
        if is_suspicious:
            viral_score = max(viral_score, SUSPICIOUS_VIRAL_SCORE_FLOOR)
        useful_for_report = (
            bool(parsed.get("useful_for_report", True)) and not is_suspicious
        )
        return enforce_paid_disclosure_policy(
            ReviewAnalysisResult(
                sentiment=str(parsed.get("sentiment", "")),
                summary=str(parsed.get("summary", "")),
                topics=[str(topic) for topic in parsed.get("topics", [])],
                viral_score=viral_score,
                quality_score=quality_score,
                is_suspicious=is_suspicious,
                useful_for_report=useful_for_report,
                pros=[str(pro) for pro in parsed.get("pros", [])],
                cons=[str(con) for con in parsed.get("cons", [])],
                detected_patterns=[
                    str(pattern) for pattern in parsed.get("detected_patterns", [])
                ],
                evidence=[str(item) for item in parsed.get("evidence", [])],
            ),
            source_text=request.body,
        )


class CrewAIReviewCrewRunner:
    def __init__(
        self,
        *,
        model: str,
        api_key: str,
        verbose: bool = False,
        current_date: str | None = None,
    ) -> None:
        self._model = model
        self._api_key = api_key
        self._verbose = verbose
        self._current_date = current_date or _today_kst()

    def run(self, *, review_id: str, body: str, rating: int | None) -> dict[str, Any]:
        try:
            from crewai import LLM, Agent, Crew, Process, Task
        except ImportError as exc:
            raise CrewAIRuntimeDependencyError(
                "crewai is required to run the multi-agent review analyzer."
            ) from exc

        llm = LLM(
            model=f"gemini/{self._model}",
            api_key=self._api_key,
            temperature=0,
        )
        viral_detector = Agent(
            role="Viral Review Detector",
            goal="Detect paid, duplicated, keyword-stuffed, or low-evidence reviews.",
            backstory=(
                "You are a strict review forensics analyst. You identify advertising patterns "
                "without deleting the original review data."
            ),
            llm=llm,
            verbose=self._verbose,
            allow_delegation=False,
        )
        report_summarizer = Agent(
            role="Review Report Summarizer",
            goal="Use only trustworthy user evidence to decide whether a review can support reports.",
            backstory=(
                "You turn detector findings into structured report inputs. Suspicious reviews "
                "must be marked useful_for_report=false."
            ),
            llm=llm,
            verbose=self._verbose,
            allow_delegation=False,
        )
        detector_task = Task(
            description=(
                "아래 리뷰의 바이럴 오염도를 분석하세요.\n"
                f"현재 날짜: {self._current_date}\n"
                f"날짜 판단은 반드시 {self._current_date} 기준으로만 하세요. "
                "예를 들어 현재 날짜보다 이전 날짜는 미래 날짜가 아닙니다.\n"
                "모든 문자열 값은 한국어로 작성하세요. 영어 라벨을 쓰지 마세요.\n"
                "viral_score는 0=오염 없음, 100=명백한 바이럴입니다. "
                "is_suspicious=true이면 viral_score는 반드시 70 이상이어야 합니다.\n"
                "quality_score는 0=근거 없음, 100=구체적인 실사용 경험이 풍부함입니다.\n"
                "#협찬, 협찬, 광고, 대가성, 제품 제공, 무상 제공, 원고료, 체험단, 서포터즈, "
                "sponsored, gifted, paid partnership 같은 대가성/협찬 표기가 본문이나 이미지 라벨에 있으면 "
                "명백한 바이럴 신호로 보고 viral_score를 높이고 quality_score를 낮추며 useful_for_report=false로 설정하세요.\n"
                "Return JSON fields: viral_score, quality_score, is_suspicious, "
                "detected_patterns, evidence.\n"
                f"Review ID: {review_id}\n"
                f"Rating: {rating if rating is not None else 'unknown'}\n"
                f"Review: {body}"
            ),
            expected_output=(
                "Strict JSON only. 모든 문자열 값은 한국어로 작성. "
                "viral_score는 0=오염 없음, 100=명백한 바이럴. "
                "is_suspicious=true이면 viral_score는 반드시 70 이상. "
                "Fields: viral_score, quality_score, is_suspicious, detected_patterns, evidence."
            ),
            agent=viral_detector,
        )
        summarizer_task = Task(
            description=(
                "detector 결과와 원문 리뷰만 사용해 최종 리포트 입력 JSON을 작성하세요.\n"
                f"현재 날짜: {self._current_date}\n"
                f"날짜 판단은 반드시 {self._current_date} 기준으로만 하세요. "
                "현재 날짜보다 이전 방문일은 미래 방문이 아닙니다.\n"
                "모든 문자열 값은 한국어로 작성하세요. sentiment, topics, summary, pros, cons, "
                "detected_patterns, evidence 모두 한국어 값이어야 합니다.\n"
                "summary는 한국어 한 문장, 80자 내외로 사용자가 읽을 핵심 경험만 간략히 요약하세요.\n"
                "viral_score는 0=오염 없음, 100=명백한 바이럴입니다. "
                "is_suspicious=true이면 viral_score는 반드시 70 이상이어야 합니다. "
                "is_suspicious=true이면 useful_for_report=false입니다. "
                "quality_score는 0=근거 없음, 100=구체적인 실사용 경험이 풍부함입니다.\n"
                "#협찬, 협찬, 광고, 대가성, 제품 제공, 무상 제공, 원고료, 체험단, 서포터즈, "
                "sponsored, gifted, paid partnership 같은 대가성/협찬 표기가 본문이나 이미지 라벨에 있으면 "
                "명백한 바이럴 신호로 보고 viral_score를 높이고 quality_score를 낮추며 useful_for_report=false로 설정하세요.\n"
                "Return final JSON fields: "
                "sentiment, summary, topics, viral_score, quality_score, is_suspicious, "
                "useful_for_report, pros, cons, detected_patterns, evidence. "
                "대가성, 반복 키워드, 과도한 홍보문체, 중복, 구체 경험 부족이면 "
                "useful_for_report=false로 설정하세요."
            ),
            expected_output=(
                "Strict JSON object only. No markdown. 모든 문자열 값은 한국어로 작성. "
                "summary는 한국어 한 문장, 80자 내외의 핵심 경험 요약. "
                "점수는 0~100 숫자. is_suspicious=true이면 viral_score>=70 및 useful_for_report=false."
            ),
            agent=report_summarizer,
            context=[detector_task],
        )
        crew = Crew(
            agents=[viral_detector, report_summarizer],
            tasks=[detector_task, summarizer_task],
            process=Process.sequential,
            verbose=self._verbose,
        )
        output = crew.kickoff()
        return _parse_crew_output(output)


def _parse_crew_output(output: Any) -> dict[str, Any]:
    if isinstance(output, dict):
        return output
    raw = getattr(output, "raw", output)
    if not isinstance(raw, str):
        raw = str(raw)
    text = raw.strip()
    if text.startswith("```"):
        lines = [
            line for line in text.splitlines() if not line.strip().startswith("```")
        ]
        text = "\n".join(lines).strip()
    return json.loads(text)


def _clamp_score(score: float) -> float:
    return max(0.0, min(100.0, score))


def _today_kst() -> str:
    return datetime.now(ZoneInfo("Asia/Seoul")).date().isoformat()
