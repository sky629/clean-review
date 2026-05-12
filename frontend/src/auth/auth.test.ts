import { describe, expect, it, vi } from "vitest";

import {
  createAuthService,
  localStorageAuthStore,
  type AuthSession
} from "./auth";

describe("auth token exchange flow", () => {
  it("exchanges a one time login code for a stored role-aware session", async () => {
    const session: AuthSession = {
      accessToken: "token-123",
      user: { id: "user-1", email: "admin@example.com", role: "ADMIN" }
    };
    const fetcher = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => session
    });
    const store = localStorageAuthStore("clean-review-test");
    const auth = createAuthService({
      loginCodeExchangeEndpoint: "/api/v1/auth/google/login-code/exchange",
      fetcher,
      store
    });

    await expect(auth.exchangeLoginCode("abc")).resolves.toEqual(session);

    expect(fetcher).toHaveBeenCalledWith("/api/v1/auth/google/login-code/exchange", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      credentials: "include",
      body: JSON.stringify({
        code: "abc"
      })
    });
    expect(auth.getSession()).toEqual(session);
  });

  it("clears persisted auth state", () => {
    const store = localStorageAuthStore("clean-review-test-clear");
    store.save({
      accessToken: "token-123",
      user: { id: "user-1", email: "user@example.com", role: "USER" }
    });

    store.clear();

    expect(store.load()).toBeNull();
  });

  it("loads spring oauth authorization url from backend", async () => {
    const fetcher = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ authorizationUrl: "http://localhost:8080/oauth2/authorization/google" })
    });
    const auth = createAuthService({
      loginCodeExchangeEndpoint: "/api/v1/auth/google/login-code/exchange",
      authorizeUrlEndpoint: "/api/v1/auth/google/authorize-url",
      fetcher,
      store: localStorageAuthStore("clean-review-test-authorize")
    });

    await expect(auth.getGoogleAuthorizationUrl("http://localhost:5173/oauth/google/callback")).resolves.toBe(
      "http://localhost:8080/oauth2/authorization/google"
    );

    expect(fetcher).toHaveBeenCalledWith(
      "/api/v1/auth/google/authorize-url?redirectUri=http%3A%2F%2Flocalhost%3A5173%2Foauth%2Fgoogle%2Fcallback",
      { credentials: "include", headers: { Accept: "application/json" } }
    );
  });

  it("refreshes and logs out through backend token endpoints", async () => {
    const refreshed: AuthSession = {
      accessToken: "new-token",
      user: { id: "user-1", email: "user@example.com", role: "USER" }
    };
    const fetcher = vi.fn()
      .mockResolvedValueOnce({
        ok: true,
        json: async () => refreshed
      })
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({})
      });
    const store = localStorageAuthStore("clean-review-test-refresh");
    store.save({
      accessToken: "old-token",
      user: { id: "user-1", email: "user@example.com", role: "USER" }
    });
    const auth = createAuthService({
      loginCodeExchangeEndpoint: "/api/v1/auth/google/login-code/exchange",
      refreshEndpoint: "/api/v1/auth/refresh",
      logoutEndpoint: "/api/v1/auth/logout",
      fetcher,
      store
    });

    await expect(auth.refresh()).resolves.toEqual(refreshed);
    await auth.signOut();

    expect(fetcher).toHaveBeenNthCalledWith(1, "/api/v1/auth/refresh", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      credentials: "include"
    });
    expect(fetcher).toHaveBeenNthCalledWith(2, "/api/v1/auth/logout", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      credentials: "include"
    });
    expect(store.load()).toBeNull();
  });
});
