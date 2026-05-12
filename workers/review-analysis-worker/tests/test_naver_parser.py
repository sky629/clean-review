from pathlib import Path

from review_analysis_worker.parsers.naver import parse_naver_reviews


def test_parse_naver_reviews_from_fixture_html() -> None:
    fixture = Path(__file__).parent / "fixtures" / "naver_blog.html"

    reviews = parse_naver_reviews(fixture.read_text(encoding="utf-8"))

    assert [review.review_id for review in reviews] == ["naver-101", "naver-102"]
    assert reviews[0].author == "summer"
    assert reviews[0].rating == 5
    assert (
        reviews[0].body == "떡이 쫄깃했고 포장도 깔끔해서 집에 와서도 먹기 좋았습니다."
    )
    assert reviews[1].created_at == "2026-05-06"
