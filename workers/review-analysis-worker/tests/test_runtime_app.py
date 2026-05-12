from review_analysis_worker.runtime.app import build_pipeline
from review_analysis_worker.runtime.config import WorkerSettings


class FakeCrewRunner:
    def run(self, *, review_id: str, body: str, rating: int | None) -> dict:
        return {
            "sentiment": "positive",
            "summary": "ok",
            "topics": [],
            "viral_score": 1,
            "quality_score": 99,
            "is_suspicious": False,
            "useful_for_report": True,
            "pros": [],
            "cons": [],
            "detected_patterns": [],
            "evidence": [],
        }


class FakeConnection:
    def execute(self, sql: str, params: tuple = ()):
        return self


def test_build_pipeline_wires_naver_api_collector_crewai_analyzer_and_postgres_storage() -> (
    None
):
    settings = WorkerSettings.from_env(
        {
            "NAVER_SEARCH_CLIENT_ID": "naver-id",
            "NAVER_SEARCH_CLIENT_SECRET": "naver-secret",
        }
    )

    pipeline = build_pipeline(
        settings=settings,
        review_crew_runner=FakeCrewRunner(),
        postgres_connection=FakeConnection(),
    )

    assert pipeline is not None


def test_build_pipeline_uses_naver_blog_playwright_collector() -> None:
    settings = WorkerSettings.from_env(
        {
            "NAVER_SEARCH_CLIENT_ID": "naver-id",
            "NAVER_SEARCH_CLIENT_SECRET": "naver-secret",
        }
    )

    pipeline = build_pipeline(
        settings=settings,
        review_crew_runner=FakeCrewRunner(),
        postgres_connection=FakeConnection(),
    )

    assert pipeline.collector_name == "PlaywrightReviewCollector"
