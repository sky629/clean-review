export type UserRole = "USER" | "ADMIN";

export type AuthUser = {
  id: string;
  email: string;
  role: UserRole;
};

export type AuthSession = {
  accessToken: string;
  user: AuthUser;
};

export type AuthStore = {
  load: () => AuthSession | null;
  save: (session: AuthSession) => void;
  clear: () => void;
};

type AuthServiceOptions = {
  loginCodeExchangeEndpoint: string;
  authorizeUrlEndpoint?: string;
  refreshEndpoint?: string;
  logoutEndpoint?: string;
  fetcher?: typeof fetch;
  store: AuthStore;
};

export function localStorageAuthStore(key = "clean-review-auth"): AuthStore {
  return {
    load() {
      const raw = window.localStorage.getItem(key);
      if (!raw) {
        return null;
      }

      try {
        return JSON.parse(raw) as AuthSession;
      } catch {
        window.localStorage.removeItem(key);
        return null;
      }
    },
    save(session) {
      window.localStorage.setItem(key, JSON.stringify(session));
    },
    clear() {
      window.localStorage.removeItem(key);
    }
  };
}

export function createAuthService({
  loginCodeExchangeEndpoint,
  authorizeUrlEndpoint,
  refreshEndpoint,
  logoutEndpoint,
  fetcher = fetch,
  store
}: AuthServiceOptions) {
  return {
    async getGoogleAuthorizationUrl(redirectUri = window.location.origin + "/oauth/google/callback") {
      if (!authorizeUrlEndpoint) {
        throw new Error("Google authorization URL endpoint is not configured.");
      }

      const response = await fetcher(
        `${authorizeUrlEndpoint}?redirectUri=${encodeURIComponent(redirectUri)}`,
        { credentials: "include", headers: { Accept: "application/json" } }
      );

      if (!response.ok) {
        throw new Error("Unable to load Google authorization URL.");
      }

      const result = (await response.json()) as { authorizationUrl: string };
      return result.authorizationUrl;
    },
    async exchangeLoginCode(code: string) {
      const response = await fetcher(loginCodeExchangeEndpoint, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        credentials: "include",
        body: JSON.stringify({ code })
      });

      if (!response.ok) {
        throw new Error("Unable to exchange login code.");
      }

      const session = (await response.json()) as AuthSession;
      store.save(session);
      return session;
    },
    async refresh() {
      if (!refreshEndpoint) {
        throw new Error("Refresh endpoint is not configured.");
      }

      const current = store.load();
      if (!current) {
        throw new Error("No auth session to refresh.");
      }

      const response = await fetcher(refreshEndpoint, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        credentials: "include"
      });

      if (!response.ok) {
        throw new Error("Unable to refresh auth session.");
      }

      const session = (await response.json()) as AuthSession;
      store.save(session);
      return session;
    },
    getSession() {
      return store.load();
    },
    async signOut() {
      const current = store.load();
      if (current && logoutEndpoint) {
        const response = await fetcher(logoutEndpoint, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          credentials: "include"
        });
        if (!response.ok) {
          throw new Error("Unable to logout auth session.");
        }
      }

      store.clear();
    }
  };
}
