import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { App } from "./App";
import type { AuthSession } from "./auth/auth";

const adminSession: AuthSession = {
  accessToken: "admin-token",
  user: { id: "admin-1", email: "admin@example.com", role: "ADMIN" }
};

afterEach(() => {
  window.localStorage.clear();
});

function renderAt(path: string, session: AuthSession | null = null) {
  window.history.pushState({}, "", path);
  render(<App initialSession={session} />);
}

function okJson(body: unknown) {
  return {
    ok: true,
    json: async () => body
  };
}

function userSession(): AuthSession {
  return {
    accessToken: "user-token",
    user: { id: "user-1", email: "user@example.com", role: "USER" }
  };
}

describe("App route shells", () => {
  it("renders login screen for anonymous root access", () => {
    renderAt("/");

    expect(screen.getByRole("banner")).toHaveTextContent("Clean Review");
    expect(screen.getByRole("heading", { name: "Login" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Continue with Google" })).toBeInTheDocument();
    expect(screen.queryByRole("heading", { name: "Review targets" })).not.toBeInTheDocument();
  });

  it("renders public shell after authentication", async () => {
    const fetcher = vi.fn().mockResolvedValue(okJson([]));
    window.history.pushState({}, "", "/");
    render(<App initialSession={userSession()} fetcher={fetcher} />);

    expect(screen.getByRole("heading", { name: "Review targets" })).toBeInTheDocument();
    expect(screen.getAllByRole("button", { name: "Review targets" })).toHaveLength(1);
    await waitFor(() => {
      expect(fetcher).toHaveBeenCalledWith("http://localhost:8080/api/v1/review-targets", {
        headers: {
          Accept: "application/json",
          Authorization: "Bearer user-token"
        }
      });
    });
  });

  it("renders review targets from the backend response", async () => {
    const fetcher = vi.fn().mockResolvedValue(
      okJson([
        {
          id: "rt-1",
          type: "PLACE",
          keyword: "성수동 파스타 맛집",
          status: "ACTIVE"
        }
      ])
    );

    window.history.pushState({}, "", "/public/review-targets");
    render(<App initialSession={userSession()} fetcher={fetcher} />);

    expect(await screen.findByText("성수동 파스타 맛집")).toBeInTheDocument();
    expect(screen.getAllByText("PLACE")).toHaveLength(2);
    expect(screen.queryByText("Name")).not.toBeInTheDocument();
  });

  it("creates a review target and appends it to the list", async () => {
    const fetcher = vi
      .fn()
      .mockResolvedValueOnce(okJson([]))
      .mockResolvedValueOnce(
        okJson({
          id: "rt-2",
          type: "PRODUCT",
          keyword: "무선 이어폰 후기",
          status: "ACTIVE"
        })
      );

    window.history.pushState({}, "", "/public/review-targets");
    render(<App initialSession={userSession()} fetcher={fetcher} />);

    fireEvent.change(screen.getByLabelText("Type"), { target: { value: "PRODUCT" } });
    expect(screen.queryByLabelText("Name")).not.toBeInTheDocument();
    fireEvent.change(screen.getByLabelText("Search keyword"), {
      target: { value: "무선 이어폰 후기" }
    });
    fireEvent.click(screen.getByRole("button", { name: "Register target" }));

    expect(await screen.findByText("무선 이어폰 후기")).toBeInTheDocument();
    expect(fetcher).toHaveBeenLastCalledWith("http://localhost:8080/api/v1/review-targets", {
      method: "POST",
      headers: {
        Accept: "application/json",
        "Content-Type": "application/json",
        Authorization: "Bearer user-token"
      },
      body: JSON.stringify({
        type: "PRODUCT",
        keyword: "무선 이어폰 후기"
      })
    });
  });

  it("requests manual resync for a review target", async () => {
    const fetcher = vi
      .fn()
      .mockResolvedValueOnce(
        okJson([
          {
            id: "rt-1",
            type: "PLACE",
            keyword: "성수동 파스타 맛집",
            status: "ACTIVE"
          }
        ])
      )
      .mockResolvedValueOnce(
        okJson({ id: "run-1", runReason: "MANUAL_RESYNC", status: "REQUESTED" })
      );

    window.history.pushState({}, "", "/public/review-targets");
    render(<App initialSession={userSession()} fetcher={fetcher} />);

    fireEvent.click(await screen.findByRole("button", { name: "Resync 성수동 파스타 맛집" }));

    await waitFor(() => {
      expect(fetcher).toHaveBeenNthCalledWith(
        2,
        "http://localhost:8080/api/v1/review-targets/rt-1/collection-runs",
        {
          method: "POST",
          headers: {
            Accept: "application/json",
            "Content-Type": "application/json",
            Authorization: "Bearer user-token"
          },
          body: JSON.stringify({})
        }
      );
    });
    expect(await screen.findByText("성수동 파스타 맛집 재수집이 접수되었습니다. 상태: REQUESTED")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Resync 성수동 파스타 맛집" })).toHaveTextContent("Queued");
  });

  it("disables resync immediately to prevent repeated clicks", async () => {
    const resyncPromise = new Promise(() => {});
    const fetcher = vi
      .fn()
      .mockResolvedValueOnce(
        okJson([
          {
            id: "rt-1",
            type: "PLACE",
            keyword: "성수동 파스타 맛집",
            status: "ACTIVE"
          }
        ])
      )
      .mockReturnValueOnce(resyncPromise);

    window.history.pushState({}, "", "/public/review-targets");
    render(<App initialSession={userSession()} fetcher={fetcher} />);

    const button = await screen.findByRole("button", { name: "Resync 성수동 파스타 맛집" });
    fireEvent.click(button);
    fireEvent.click(button);

    expect(button).toBeDisabled();
    expect(button).toHaveTextContent("Requesting...");
    expect(screen.getByText("성수동 파스타 맛집 재수집 요청을 보내는 중입니다.")).toBeInTheDocument();
    expect(fetcher).toHaveBeenCalledTimes(2);
  });

  it("shows an error when manual resync fails", async () => {
    const fetcher = vi
      .fn()
      .mockResolvedValueOnce(
        okJson([
          {
            id: "rt-1",
            type: "PLACE",
            keyword: "성수동 파스타 맛집",
            status: "ACTIVE"
          }
        ])
      )
      .mockResolvedValueOnce({ ok: false, json: async () => ({}) });

    window.history.pushState({}, "", "/public/review-targets");
    render(<App initialSession={userSession()} fetcher={fetcher} />);

    fireEvent.click(await screen.findByRole("button", { name: "Resync 성수동 파스타 맛집" }));

    expect(
      await screen.findByText("성수동 파스타 맛집 재수집 요청에 실패했습니다. 다시 로그인하거나 잠시 후 재시도하세요.")
    ).toBeInTheDocument();
  });

  it("loads collected reviews for the selected review target", async () => {
    const fetcher = vi
      .fn()
      .mockResolvedValueOnce(
        okJson([
          {
            id: "rt-1",
            type: "PLACE",
            keyword: "성수동 파스타 맛집",
            status: "ACTIVE"
          }
        ])
      )
      .mockResolvedValueOnce(
        okJson([
          {
            id: "review-1",
            targetId: "rt-1",
            source: "NAVER_BLOG",
            sourceReviewId: "naver-101",
            canonicalUrl: "https://blog.naver.com/reviews/naver-101",
            title: null,
            rawText:
              "웨이팅은 20분이었고 포장 상태가 좋았습니다. 이 문장은 화면에 그대로 길게 노출되면 안 되는 원문 상세 내용입니다.",
            summary: "웨이팅과 포장 상태가 구체적으로 언급된 후기입니다.",
            publishedAt: "2026-05-06T00:00:00Z",
            status: "COLLECTED",
            viralScore: 8,
            qualityScore: 88,
            isSuspicious: false,
            usefulForReport: true,
            detectedPatterns: [],
            evidence: ["웨이팅은 20분"]
          }
        ])
      );

    window.history.pushState({}, "", "/public/review-targets");
    render(<App initialSession={userSession()} fetcher={fetcher} />);

    fireEvent.click(await screen.findByRole("button", { name: "View reviews for 성수동 파스타 맛집" }));

    expect(await screen.findByText("웨이팅과 포장 상태가 구체적으로 언급된 후기입니다.")).toBeInTheDocument();
    expect(screen.queryByText("웨이팅은 20분")).not.toBeInTheDocument();
    expect(screen.queryByText(/원문 상세 내용/)).not.toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Open review" })).toHaveAttribute(
      "href",
      "https://blog.naver.com/reviews/naver-101"
    );
    expect(screen.getByRole("heading", { name: "Reviews for 성수동 파스타 맛집" })).toBeInTheDocument();
    expect(screen.getByText("Included")).toBeInTheDocument();
    expect(fetcher).toHaveBeenNthCalledWith(2, "http://localhost:8080/api/v1/review-targets/rt-1/reviews", {
      headers: {
        Accept: "application/json",
        Authorization: "Bearer user-token"
      }
    });
  });

  it("shows pending report state when a collected review has no analysis yet", async () => {
    const fetcher = vi
      .fn()
      .mockResolvedValueOnce(
        okJson([
          {
            id: "rt-1",
            type: "PRODUCT",
            keyword: "아이폰 17",
            status: "ACTIVE"
          }
        ])
      )
      .mockResolvedValueOnce(
        okJson([
          {
            id: "review-1",
            targetId: "rt-1",
            source: "NAVER_BLOG",
            sourceReviewId: "naver-101",
            canonicalUrl: "https://blog.naver.com/reviews/naver-101",
            title: null,
            rawText: "아이폰17 가격 비교 후기입니다.",
            summary: null,
            publishedAt: "2026-05-06T00:00:00Z",
            status: "COLLECTED",
            viralScore: null,
            qualityScore: null,
            isSuspicious: null,
            usefulForReport: null,
            detectedPatterns: [],
            evidence: []
          }
        ])
      );

    window.history.pushState({}, "", "/public/review-targets");
    render(<App initialSession={userSession()} fetcher={fetcher} />);

    fireEvent.click(await screen.findByRole("button", { name: "View reviews for 아이폰 17" }));

    expect(await screen.findByText("Pending")).toBeInTheDocument();
    expect(screen.queryByText("Excluded")).not.toBeInTheDocument();
  });

  it("refreshes a persisted session before loading review targets", async () => {
    window.localStorage.setItem(
      "clean-review-auth",
      JSON.stringify({
        accessToken: "expired-token",
        user: { id: "user-1", email: "user@example.com", role: "USER" }
      })
    );
    const fetcher = vi
      .fn()
      .mockResolvedValueOnce(
        okJson({
          accessToken: "fresh-token",
          user: { id: "user-1", email: "user@example.com", role: "USER" }
        })
      )
      .mockResolvedValueOnce(
        okJson([
          {
            id: "rt-1",
            type: "PLACE",
            keyword: "성수동 파스타 맛집",
            status: "ACTIVE"
          }
        ])
      );

    window.history.pushState({}, "", "/public/review-targets");
    render(<App fetcher={fetcher} />);

    expect(await screen.findByText("성수동 파스타 맛집")).toBeInTheDocument();
    expect(fetcher).toHaveBeenNthCalledWith(1, "http://localhost:8080/api/v1/auth/refresh", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      credentials: "include"
    });
    expect(fetcher).toHaveBeenNthCalledWith(2, "http://localhost:8080/api/v1/review-targets", {
      headers: {
        Accept: "application/json",
        Authorization: "Bearer fresh-token"
      }
    });
  });

  it("blocks admin pages for non-admin users", () => {
    renderAt("/admin/reviews", {
      accessToken: "user-token",
      user: { id: "user-1", email: "user@example.com", role: "USER" }
    });

    expect(screen.getByRole("heading", { name: "Access required" })).toBeInTheDocument();
  });

  it("exchanges oauth callback code and enters the app", async () => {
    const fetcher = vi
      .fn()
      .mockResolvedValueOnce(
        okJson({
          accessToken: "callback-token",
          user: { id: "user-1", email: "user@example.com", role: "USER" }
        })
      )
      .mockResolvedValueOnce(okJson([]));

    window.history.pushState({}, "", "/oauth/google/callback?code=login-code-1");
    render(<App initialSession={null} fetcher={fetcher} />);

    await waitFor(() => {
      expect(screen.getByRole("heading", { name: "Review targets" })).toBeInTheDocument();
    });
    expect(fetcher).toHaveBeenNthCalledWith(1, "http://localhost:8080/api/v1/auth/google/login-code/exchange", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      credentials: "include",
      body: JSON.stringify({ code: "login-code-1" })
    });
    expect(window.location.pathname).toBe("/public/review-targets");
  });

  it.each([
    ["/admin/review-targets", "ReviewTargets"],
    ["/admin/reviews", "Reviews"],
    ["/admin/collection-runs", "CollectionRuns"],
    ["/admin/analysis-runs", "AnalysisRuns"],
    ["/admin/retry-jobs", "RetryJobs"],
    ["/admin/dead-letters", "DeadLetters"],
    ["/admin/notifications", "Notifications"]
  ])("renders readonly admin page %s", async (path, title) => {
    const fetcher = vi.fn().mockResolvedValue(okJson([{ id: "admin-row-1", status: "ACTIVE" }]));
    window.history.pushState({}, "", path);
    render(<App initialSession={adminSession} fetcher={fetcher} />);

    expect(screen.getByRole("navigation", { name: "Admin" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: title })).toBeInTheDocument();
    expect(screen.getByText("Read only")).toBeInTheDocument();
    expect(await screen.findByText("admin-row-1")).toBeInTheDocument();
  });

  it("renders collection failure reason on the admin collection runs page", async () => {
    const fetcher = vi.fn().mockResolvedValue(
      okJson([
        {
          id: "run-1",
          targetId: "target-1",
          source: "NAVER_BLOG",
          keyword: "성수동 파스타",
          status: "FAILED",
          requestedAt: "2026-05-11T00:00:00Z",
          failureCode: "CollectionBlocked",
          failureMessage: "Naver blocked the collection page."
        }
      ])
    );

    window.history.pushState({}, "", "/admin/collection-runs");
    render(<App initialSession={adminSession} fetcher={fetcher} />);

    expect(await screen.findByText("CollectionBlocked")).toBeInTheDocument();
    expect(screen.getByText("Naver blocked the collection page.")).toBeInTheDocument();
  });

  it("renders retry failure reason on the admin retry jobs page", async () => {
    const fetcher = vi.fn().mockResolvedValue(
      okJson([
        {
          id: "retry-1",
          topic: "review.collection.requested",
          eventType: "ReviewCollectionRequested",
          attempt: 2,
          maxAttempts: 3,
          nextAttemptAt: "2026-05-11T00:00:00Z",
          status: "SCHEDULED",
          lastErrorCode: "CollectionBlocked",
          lastErrorMessage: "Google returned a blocked search page."
        }
      ])
    );

    window.history.pushState({}, "", "/admin/retry-jobs");
    render(<App initialSession={adminSession} fetcher={fetcher} />);

    expect(await screen.findByText("CollectionBlocked")).toBeInTheDocument();
    expect(screen.getByText("Google returned a blocked search page.")).toBeInTheDocument();
  });
});
