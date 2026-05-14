from __future__ import annotations

import json
from dataclasses import dataclass
from datetime import datetime
from typing import Protocol
from zoneinfo import ZoneInfo

DEFAULT_GEMINI_MODEL = "gemini-2.5-flash"
SUSPICIOUS_VIRAL_SCORE_FLOOR = 70.0
PAID_DISCLOSURE_VIRAL_SCORE_FLOOR = 85.0
PAID_DISCLOSURE_QUALITY_SCORE_CEILING = 40.0
PAID_DISCLOSURE_PATTERN = "대가성/협찬 표기"
PAID_DISCLOSURE_KEYWORDS = [
    "#협찬",
    "협찬",
    "광고",
    "대가성",
    "제품 제공",
    "제품제공",
    "무상 제공",
    "무상제공",
    "원고료",
    "체험단",
    "서포터즈",
    "sponsored",
    "sponsor",
    "gifted",
    "paid partnership",
    "#ad",
]


class GeminiClient(Protocol):
    def generate_content(self, *, model: str, prompt: str) -> str:
        """Generate analysis text for a review prompt."""


@dataclass(frozen=True, slots=True)
class ReviewAnalysisRequest:
    review_id: str
    body: str
    rating: int | None = None


@dataclass(frozen=True, slots=True)
class ReviewAnalysisResult:
    sentiment: str
    summary: str
    topics: list[str]
    viral_score: float
    quality_score: float
    is_suspicious: bool
    useful_for_report: bool
    pros: list[str]
    cons: list[str]
    detected_patterns: list[str]
    evidence: list[str]


class GeminiAnalyzer:
    def __init__(
        self,
        client: GeminiClient,
        model: str = DEFAULT_GEMINI_MODEL,
    ) -> None:
        self._client = client
        self._model = model

    @property
    def model(self) -> str:
        return self._model

    def analyze(self, request: ReviewAnalysisRequest) -> ReviewAnalysisResult:
        raw = self._client.generate_content(
            model=self._model,
            prompt=_build_prompt(request),
        )
        try:
            parsed = json.loads(_extract_json_object(raw))
        except (TypeError, ValueError, json.JSONDecodeError) as exc:
            return enforce_paid_disclosure_policy(
                _fallback_analysis(request=request, failure=exc),
                source_text=request.body,
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


def _build_prompt(request: ReviewAnalysisRequest) -> str:
    current_date = _today_kst()
    return (
        "아래 고객 리뷰의 바이럴 오염도와 실제 사용자 경험 근거를 분석하세요. "
        f"현재 날짜: {current_date}. "
        f"날짜 판단은 반드시 {current_date} 기준으로만 하세요. "
        "현재 날짜보다 이전 방문일은 미래 방문이 아닙니다. "
        "응답은 반드시 JSON만 반환하고 sentiment, summary, topics, viral_score, quality_score, "
        "is_suspicious, useful_for_report, pros, cons, detected_patterns, evidence 필드를 포함하세요. "
        "모든 문자열 값은 한국어로 작성하세요. 영어 라벨을 쓰지 마세요. "
        "summary는 한국어 한 문장, 80자 내외로 사용자가 읽을 핵심 경험만 간략히 요약하세요. "
        "viral_score는 0=오염 없음, 100=명백한 바이럴입니다. "
        "is_suspicious=true이면 viral_score는 반드시 70 이상이어야 합니다. "
        "is_suspicious=true이면 useful_for_report=false입니다. "
        "quality_score는 0=근거 없음, 100=구체적인 실사용 경험이 풍부함입니다. "
        "#협찬, 협찬, 광고, 대가성, 제품 제공, 무상 제공, 원고료, 체험단, 서포터즈, "
        "sponsored, gifted, paid partnership 같은 대가성/협찬 표기가 본문이나 이미지 라벨에 있으면 "
        "명백한 바이럴 신호로 보고 viral_score를 높이고 quality_score를 낮추며 useful_for_report=false로 설정하세요. "
        "대가성, 중복, 키워드 반복, 근거 부족 리뷰는 useful_for_report=false로 설정하세요.\n"
        f"Review ID: {request.review_id}\n"
        f"Rating: {request.rating if request.rating is not None else 'unknown'}\n"
        f"Review: {request.body}"
    )


def enforce_paid_disclosure_policy(
    result: ReviewAnalysisResult, *, source_text: str
) -> ReviewAnalysisResult:
    evidence = _paid_disclosure_evidence(source_text)
    if not evidence:
        return result

    detected_patterns = list(result.detected_patterns)
    if PAID_DISCLOSURE_PATTERN not in detected_patterns:
        detected_patterns.append(PAID_DISCLOSURE_PATTERN)

    result_evidence = list(result.evidence)
    if evidence not in result_evidence:
        result_evidence.insert(0, evidence)

    return ReviewAnalysisResult(
        sentiment=result.sentiment,
        summary=result.summary,
        topics=result.topics,
        viral_score=max(result.viral_score, PAID_DISCLOSURE_VIRAL_SCORE_FLOOR),
        quality_score=min(result.quality_score, PAID_DISCLOSURE_QUALITY_SCORE_CEILING),
        is_suspicious=True,
        useful_for_report=False,
        pros=result.pros,
        cons=result.cons,
        detected_patterns=detected_patterns,
        evidence=result_evidence,
    )


def _fallback_analysis(
    *, request: ReviewAnalysisRequest, failure: Exception
) -> ReviewAnalysisResult:
    return ReviewAnalysisResult(
        sentiment="neutral",
        summary=_fallback_summary(request.body),
        topics=[],
        viral_score=SUSPICIOUS_VIRAL_SCORE_FLOOR,
        quality_score=0.0,
        is_suspicious=True,
        useful_for_report=False,
        pros=[],
        cons=[],
        detected_patterns=["Gemini JSON 파싱 실패"],
        evidence=[f"{type(failure).__name__}: {str(failure)[:120]}"],
    )


def _fallback_summary(text: str) -> str:
    normalized = " ".join(text.split())
    if len(normalized) <= 80:
        return normalized
    return normalized[:79].rstrip() + "..."


def _paid_disclosure_evidence(text: str) -> str:
    normalized = " ".join(text.split())
    lowered = normalized.casefold()
    for keyword in PAID_DISCLOSURE_KEYWORDS:
        lowered_keyword = keyword.casefold()
        index = lowered.find(lowered_keyword)
        if index == -1:
            continue
        start = max(0, index - 35)
        end = min(len(normalized), index + len(keyword) + 35)
        return normalized[start:end]
    return ""


def _clamp_score(score: float) -> float:
    return max(0.0, min(100.0, score))


def _extract_json_object(raw: str) -> str:
    text = raw.strip()
    if text.startswith("```"):
        lines = text.splitlines()
        if lines and lines[0].startswith("```"):
            lines = lines[1:]
        if lines and lines[-1].strip() == "```":
            lines = lines[:-1]
        text = "\n".join(lines).strip()

    start = text.find("{")
    end = text.rfind("}")
    if start == -1 or end == -1 or end < start:
        raise ValueError("Gemini response did not contain a JSON object.")
    return text[start : end + 1]


def _today_kst() -> str:
    return datetime.now(ZoneInfo("Asia/Seoul")).date().isoformat()
