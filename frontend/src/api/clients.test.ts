import { describe, expect, it, vi } from "vitest";

import { createAdminApiClient, createPublicApiClient } from "./clients";

describe("API clients", () => {
  it("fetches user review targets with bearer auth", async () => {
    const fetcher = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => [{ id: "rt-1", keyword: "성수동 파스타 맛집", status: "active" }]
    });
    const client = createPublicApiClient({
      baseUrl: "/api/v1",
      fetcher,
      getAccessToken: () => "user-token"
    });

    await expect(client.listReviewTargets()).resolves.toEqual([
      { id: "rt-1", keyword: "성수동 파스타 맛집", status: "active" }
    ]);

    expect(fetcher).toHaveBeenCalledWith("/api/v1/review-targets", {
      headers: {
        Accept: "application/json",
        Authorization: "Bearer user-token"
      }
    });
  });

  it("creates a review target with bearer auth", async () => {
    const fetcher = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ id: "rt-1", keyword: "성수동 파스타 맛집", status: "ACTIVE" })
    });
    const client = createPublicApiClient({
      baseUrl: "/api/v1",
      fetcher,
      getAccessToken: () => "user-token"
    });

    await expect(
      client.createReviewTarget({
        type: "PLACE",
        keyword: "성수동 파스타 맛집"
      })
    ).resolves.toEqual({ id: "rt-1", keyword: "성수동 파스타 맛집", status: "ACTIVE" });

    expect(fetcher).toHaveBeenCalledWith("/api/v1/review-targets", {
      method: "POST",
      headers: {
        Accept: "application/json",
        "Content-Type": "application/json",
        Authorization: "Bearer user-token"
      },
      body: JSON.stringify({
        type: "PLACE",
        keyword: "성수동 파스타 맛집"
      })
    });
  });

  it("fetches the latest report for a review target", async () => {
    const fetcher = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ id: "report-1", trustScore: 88 })
    });
    const client = createPublicApiClient({
      baseUrl: "/api/v1",
      fetcher,
      getAccessToken: () => "user-token"
    });

    await expect(client.getLatestReviewReport("rt-1")).resolves.toEqual({
      id: "report-1",
      trustScore: 88
    });

    expect(fetcher).toHaveBeenCalledWith("/api/v1/review-targets/rt-1/report", {
      headers: {
        Accept: "application/json",
        Authorization: "Bearer user-token"
      }
    });
  });

  it("fetches collected reviews for a review target", async () => {
    const fetcher = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => [{ id: "review-1", rawText: "웨이팅은 20분", usefulForReport: true }]
    });
    const client = createPublicApiClient({
      baseUrl: "/api/v1",
      fetcher,
      getAccessToken: () => "user-token"
    });

    await expect(client.listReviewTargetReviews("rt-1")).resolves.toEqual([
      { id: "review-1", rawText: "웨이팅은 20분", usefulForReport: true }
    ]);

    expect(fetcher).toHaveBeenCalledWith("/api/v1/review-targets/rt-1/reviews", {
      headers: {
        Accept: "application/json",
        Authorization: "Bearer user-token"
      }
    });
  });

  it("requests manual collection for a review target", async () => {
    const fetcher = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ id: "run-1", runReason: "MANUAL_RESYNC" })
    });
    const client = createPublicApiClient({
      baseUrl: "/api/v1",
      fetcher,
      getAccessToken: () => "user-token"
    });

    await expect(client.requestReviewTargetCollection("rt-1")).resolves.toEqual({
      id: "run-1",
      runReason: "MANUAL_RESYNC"
    });

    expect(fetcher).toHaveBeenCalledWith("/api/v1/review-targets/rt-1/collection-runs", {
      method: "POST",
      headers: {
        Accept: "application/json",
        "Content-Type": "application/json",
        Authorization: "Bearer user-token"
      },
      body: JSON.stringify({})
    });
  });

  it("sends bearer auth for readonly admin resources", async () => {
    const fetcher = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => [{ id: "review-1", status: "queued" }]
    });
    const client = createAdminApiClient({
      baseUrl: "/api/admin",
      getAccessToken: () => "admin-token",
      fetcher
    });

    await expect(client.listReviews()).resolves.toEqual([
      { id: "review-1", status: "queued" }
    ]);

    expect(fetcher).toHaveBeenCalledWith("/api/admin/reviews", {
      headers: {
        Accept: "application/json",
        Authorization: "Bearer admin-token"
      }
    });
  });

  it("fetches admin notification deliveries from the backend admin prefix", async () => {
    const fetcher = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => [{ id: "delivery-1", status: "SENT" }]
    });
    const client = createAdminApiClient({
      baseUrl: "/admin/api/v1",
      getAccessToken: () => "admin-token",
      fetcher
    });

    await expect(client.listNotifications()).resolves.toEqual([
      { id: "delivery-1", status: "SENT" }
    ]);

    expect(fetcher).toHaveBeenCalledWith("/admin/api/v1/notification-deliveries", {
      headers: {
        Accept: "application/json",
        Authorization: "Bearer admin-token"
      }
    });
  });
});
