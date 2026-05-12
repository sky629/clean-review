import { useEffect, useMemo, useRef, useState, type FormEvent } from "react";

import "./styles.css";
import {
  createAuthService,
  localStorageAuthStore,
  type AuthSession
} from "./auth/auth";
import {
  createAdminApiClient,
  createPublicApiClient,
  type ApiRecord,
  type CreateReviewTargetPayload,
  type Review,
  type ReviewTarget,
  type ReviewTargetType
} from "./api/clients";
import { adminRoutes, canAccessRoute, getRouteForPath, publicRoutes } from "./routing/routes";

type AppProps = {
  initialSession?: AuthSession | null;
  fetcher?: typeof fetch;
};

type TablePage = {
  title: string;
  description: string;
  columns: string[];
};

type ResyncFeedback = {
  targetId: string;
  type: "queued" | "error";
  message: string;
};

const adminPages: Record<string, TablePage> = {
  "/admin/review-targets": {
    title: "ReviewTargets",
    description: "Configured surfaces eligible for review collection.",
    columns: ["id", "keyword", "type", "status", "createdBy"]
  },
  "/admin/reviews": {
    title: "Reviews",
    description: "Inbound review records and processing state.",
    columns: ["id", "targetId", "source", "status", "viralScore", "qualityScore", "usefulForReport", "rawText"]
  },
  "/admin/collection-runs": {
    title: "CollectionRuns",
    description: "Readonly collection executions across review sources.",
    columns: ["id", "targetId", "source", "keyword", "status", "failureCode", "failureMessage", "requestedAt"]
  },
  "/admin/analysis-runs": {
    title: "AnalysisRuns",
    description: "Analysis jobs that transform reviews into signals.",
    columns: ["id", "reviewId", "modelName", "status", "viralScore", "qualityScore", "usefulForReport", "analyzedAt"]
  },
  "/admin/retry-jobs": {
    title: "RetryJobs",
    description: "Retryable failures awaiting another attempt.",
    columns: [
      "id",
      "topic",
      "eventType",
      "attempt",
      "maxAttempts",
      "nextAttemptAt",
      "status",
      "lastErrorCode",
      "lastErrorMessage"
    ]
  },
  "/admin/dead-letters": {
    title: "DeadLetters",
    description: "Terminal failures retained for investigation.",
    columns: ["id", "sourceTopic", "eventType", "consumerName", "errorCode", "errorMessage", "failedAt"]
  },
  "/admin/notifications": {
    title: "Notifications",
    description: "Outbound notification attempts and delivery state.",
    columns: ["id", "notificationType", "channel", "recipient", "status", "sourceEventId", "createdAt"]
  }
};

const publicNavRoutes = [publicRoutes[1]];
const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

export function App({ initialSession, fetcher = fetch }: AppProps) {
  const auth = useMemo(
    () =>
      createAuthService({
        loginCodeExchangeEndpoint: `${apiBaseUrl}/api/v1/auth/google/login-code/exchange`,
        refreshEndpoint: `${apiBaseUrl}/api/v1/auth/refresh`,
        logoutEndpoint: `${apiBaseUrl}/api/v1/auth/logout`,
        fetcher,
        store: localStorageAuthStore()
      }),
    [fetcher]
  );
  const restoredSession = initialSession === undefined ? auth.getSession() : initialSession;
  const [session, setSession] = useState<AuthSession | null>(restoredSession);
  const [isRestoringSession, setIsRestoringSession] = useState(
    initialSession === undefined && restoredSession !== null
  );
  const [path, setPath] = useState(window.location.pathname);
  const [loginError, setLoginError] = useState(false);
  const route = getRouteForPath(path);
  const role = session?.user.role ?? null;

  useEffect(() => {
    if (!isRestoringSession || !session) {
      return;
    }

    let cancelled = false;
    auth.refresh()
      .then((nextSession) => {
        if (!cancelled) {
          setSession(nextSession);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setSession(null);
        }
      })
      .finally(() => {
        if (!cancelled) {
          setIsRestoringSession(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [auth, isRestoringSession]);

  useEffect(() => {
    if (path !== "/oauth/google/callback") {
      return;
    }

    const code = new URLSearchParams(window.location.search).get("code");
    if (!code) {
      setLoginError(true);
      return;
    }

    let cancelled = false;
    auth.exchangeLoginCode(code)
      .then((nextSession) => {
        if (cancelled) {
          return;
        }
        setSession(nextSession);
        window.history.replaceState({}, "", "/public/review-targets");
        setPath("/public/review-targets");
      })
      .catch(() => {
        if (!cancelled) {
          setLoginError(true);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [auth, path]);

  useEffect(() => {
    const onPopState = () => setPath(window.location.pathname);
    window.addEventListener("popstate", onPopState);
    return () => window.removeEventListener("popstate", onPopState);
  }, []);

  function navigate(nextPath: string) {
    window.history.pushState({}, "", nextPath);
    setPath(nextPath);
  }

  if (!session) {
    if (path === "/oauth/google/callback" && !loginError) {
      return <LoginCallbackShell />;
    }
    return <LoginShell error={loginError} />;
  }

  if (isRestoringSession) {
    return <LoginCallbackShell />;
  }

  if (!canAccessRoute(role, path)) {
    return <AccessRequired onNavigate={navigate} />;
  }

  if (route.shell === "admin") {
    return (
      <AdminShell
        activePath={route.path}
        fetcher={fetcher}
        onNavigate={navigate}
        session={session}
      />
    );
  }

  return (
    <PublicShell
      activePath={route.path}
      fetcher={fetcher}
      onNavigate={navigate}
      session={session}
    />
  );
}

function LoginShell({ error }: { error: boolean }) {
  function startGoogleLogin() {
    window.location.assign(`${apiBaseUrl}/oauth2/authorization/google`);
  }

  return (
    <div className="appFrame">
      <header className="topbar" role="banner">
        <span className="brandButton">Clean Review</span>
      </header>
      <main className="loginPage">
        <section className="loginPanel">
          <h1>Login</h1>
          {error ? <p className="errorText">Login failed. Try again.</p> : null}
          <button type="button" onClick={startGoogleLogin}>
            Continue with Google
          </button>
        </section>
      </main>
    </div>
  );
}

function LoginCallbackShell() {
  return (
    <div className="appFrame">
      <header className="topbar" role="banner">
        <span className="brandButton">Clean Review</span>
      </header>
      <main className="loginPage">
        <section className="loginPanel">
          <h1>Completing login</h1>
        </section>
      </main>
    </div>
  );
}

function Header({ onNavigate }: { onNavigate: (path: string) => void }) {
  return (
    <header className="topbar" role="banner">
      <button className="brandButton" type="button" onClick={() => onNavigate("/")}>
        Clean Review
      </button>
      <nav aria-label="Primary">
        <button type="button" onClick={() => onNavigate("/public/review-targets")}>
          Public
        </button>
        <button type="button" onClick={() => onNavigate("/admin/reviews")}>
          Admin
        </button>
      </nav>
    </header>
  );
}

function PublicShell({
  activePath,
  fetcher,
  onNavigate,
  session
}: {
  activePath: string;
  fetcher: typeof fetch;
  onNavigate: (path: string) => void;
  session: AuthSession;
}) {
  const api = useMemo(
    () =>
      createPublicApiClient({
        baseUrl: `${apiBaseUrl}/api/v1`,
        fetcher,
        getAccessToken: () => session.accessToken
      }),
    [fetcher, session.accessToken]
  );
  const [targets, setTargets] = useState<ReviewTarget[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [selectedTarget, setSelectedTarget] = useState<ReviewTarget | null>(null);
  const [reviews, setReviews] = useState<Review[]>([]);
  const [isLoadingReviews, setIsLoadingReviews] = useState(false);
  const [reviewsError, setReviewsError] = useState<string | null>(null);
  const [resyncingTargetId, setResyncingTargetId] = useState<string | null>(null);
  const [resyncFeedback, setResyncFeedback] = useState<ResyncFeedback | null>(null);
  const resyncRequestLocks = useRef<Set<string>>(new Set());

  useEffect(() => {
    let cancelled = false;
    setIsLoading(true);
    setError(null);

    api.listReviewTargets()
      .then((result) => {
        if (!cancelled) {
          setTargets(result);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setError("리뷰 대상을 불러오지 못했습니다.");
        }
      })
      .finally(() => {
        if (!cancelled) {
          setIsLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [api]);

  function registerTarget(payload: CreateReviewTargetPayload) {
    setIsSubmitting(true);
    setError(null);

    api.createReviewTarget(payload)
      .then((target) => {
        setTargets((current) => [target, ...current]);
      })
      .catch(() => {
        setError("리뷰 대상을 등록하지 못했습니다.");
      })
      .finally(() => {
        setIsSubmitting(false);
      });
  }

  function loadReviews(target: ReviewTarget) {
    setSelectedTarget(target);
    setReviews([]);
    setReviewsError(null);
    setIsLoadingReviews(true);

    api.listReviewTargetReviews(target.id)
      .then((result) => {
        setReviews(result);
      })
      .catch(() => {
        setReviewsError("리뷰를 불러오지 못했습니다.");
      })
      .finally(() => {
        setIsLoadingReviews(false);
      });
  }

  function requestResync(target: ReviewTarget) {
    if (
      resyncRequestLocks.current.has(target.id) ||
      resyncingTargetId === target.id ||
      (resyncFeedback?.targetId === target.id && resyncFeedback.type === "queued")
    ) {
      return;
    }

    resyncRequestLocks.current.add(target.id);
    setError(null);
    setResyncFeedback({
      targetId: target.id,
      type: "queued",
      message: `${target.keyword} 재수집 요청을 보내는 중입니다.`
    });
    setResyncingTargetId(target.id);

    api.requestReviewTargetCollection(target.id)
      .then((collectionRun) => {
        setResyncFeedback({
          targetId: target.id,
          type: "queued",
          message: `${target.keyword} 재수집이 접수되었습니다. 상태: ${collectionRun.status}`
        });
        window.setTimeout(() => {
          setResyncFeedback((current) => (current?.targetId === target.id ? null : current));
        }, 15000);
        if (selectedTarget?.id === target.id) {
          loadReviews(target);
        }
      })
      .catch(() => {
        setResyncFeedback({
          targetId: target.id,
          type: "error",
          message: `${target.keyword} 재수집 요청에 실패했습니다. 다시 로그인하거나 잠시 후 재시도하세요.`
        });
      })
      .finally(() => {
        resyncRequestLocks.current.delete(target.id);
        setResyncingTargetId(null);
      });
  }

  return (
    <div className="appFrame">
      <Header onNavigate={onNavigate} />
      <main className="page">
        <aside className="sidebar">
          <nav aria-label="Public">
            {publicNavRoutes.map((route) => (
              <button
                className={
                  activePath === route.path || activePath === "/"
                    ? "navItem active"
                    : "navItem"
                }
                key={route.path}
                type="button"
                onClick={() => onNavigate(route.path)}
              >
                {route.label}
              </button>
            ))}
          </nav>
        </aside>
        <section className="content">
          <PageHeader
            title="Review targets"
            description="Your registered places and products for review analysis."
          />
          <ReviewTargetForm isSubmitting={isSubmitting} onSubmit={registerTarget} />
          {error ? <p className="errorText">{error}</p> : null}
          {isLoading ? (
            <p>Loading review targets...</p>
          ) : (
            <ReviewTargetTable
              onRequestResync={requestResync}
              onSelectReviews={loadReviews}
              resyncFeedback={resyncFeedback}
              resyncingTargetId={resyncingTargetId}
              targets={targets}
            />
          )}
          {selectedTarget ? (
            <ReviewListPanel
              isLoading={isLoadingReviews}
              reviews={reviews}
              error={reviewsError}
              target={selectedTarget}
            />
          ) : null}
        </section>
      </main>
    </div>
  );
}

function ReviewTargetForm({
  isSubmitting,
  onSubmit
}: {
  isSubmitting: boolean;
  onSubmit: (payload: CreateReviewTargetPayload) => void;
}) {
  const [type, setType] = useState<ReviewTargetType>("PLACE");
  const [keyword, setKeyword] = useState("");

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!keyword.trim()) {
      return;
    }

    onSubmit({
      type,
      keyword: keyword.trim()
    });
    setKeyword("");
  }

  return (
    <form className="targetForm" onSubmit={submit}>
      <label>
        <span>Type</span>
        <select value={type} onChange={(event) => setType(event.target.value as ReviewTargetType)}>
          <option value="PLACE">PLACE</option>
          <option value="PRODUCT">PRODUCT</option>
        </select>
      </label>
      <label>
        <span>Search keyword</span>
        <input value={keyword} onChange={(event) => setKeyword(event.target.value)} />
      </label>
      <button type="submit" disabled={isSubmitting}>
        Register target
      </button>
    </form>
  );
}

function ReviewTargetTable({
  onRequestResync,
  onSelectReviews,
  resyncFeedback,
  resyncingTargetId,
  targets
}: {
  onRequestResync: (target: ReviewTarget) => void;
  onSelectReviews: (target: ReviewTarget) => void;
  resyncFeedback: ResyncFeedback | null;
  resyncingTargetId: string | null;
  targets: ReviewTarget[];
}) {
  if (targets.length === 0) {
    return <p>No review targets registered.</p>;
  }

  return (
    <table>
      <thead>
        <tr>
          <th>Keyword</th>
          <th>Type</th>
          <th>Status</th>
          <th>Reviews</th>
          <th>Resync</th>
        </tr>
      </thead>
      <tbody>
        {targets.map((target) => (
          <tr key={target.id}>
            <td>{target.keyword}</td>
            <td>{target.type}</td>
            <td>{target.status}</td>
            <td>
              <button
                className="linkButton"
                type="button"
                aria-label={`View reviews for ${target.keyword}`}
                onClick={() => onSelectReviews(target)}
              >
                View reviews
              </button>
            </td>
            <td>
              {resyncFeedback?.targetId === target.id ? (
                <p
                  className={
                    resyncFeedback.type === "error"
                      ? "rowStatus errorText"
                      : "rowStatus successText"
                  }
                  role="status"
                >
                  {resyncFeedback.message}
                </p>
              ) : null}
              <button
                className="linkButton"
                type="button"
                disabled={
                  resyncingTargetId === target.id ||
                  (resyncFeedback?.targetId === target.id && resyncFeedback.type === "queued")
                }
                aria-label={`Resync ${target.keyword}`}
                onClick={() => onRequestResync(target)}
              >
                {resyncingTargetId === target.id
                  ? "Requesting..."
                  : resyncFeedback?.targetId === target.id && resyncFeedback.type === "queued"
                    ? "Queued"
                    : "Resync"}
              </button>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

function ReviewListPanel({
  error,
  isLoading,
  reviews,
  target
}: {
  error: string | null;
  isLoading: boolean;
  reviews: Review[];
  target: ReviewTarget;
}) {
  return (
    <section className="detailPanel">
      <h2>Reviews for {target.keyword}</h2>
      <span className="modeLabel">Report eligibility</span>
      {error ? <p className="errorText">{error}</p> : null}
      {isLoading ? <p>Loading reviews...</p> : null}
      {!isLoading && reviews.length === 0 ? <p>No collected reviews yet.</p> : null}
      {reviews.length > 0 ? (
        <table>
          <thead>
            <tr>
              <th>Source</th>
              <th>Review</th>
              <th>Viral</th>
              <th>Quality</th>
              <th>Report</th>
            </tr>
          </thead>
          <tbody>
            {reviews.map((review) => (
              <tr key={review.id}>
                <td>{review.source}</td>
                <td>
                  <div className="reviewSummary">
                    <span>{reviewSummaryText(review)}</span>
                    <a
                      className="reviewLink"
                      href={review.canonicalUrl}
                      rel="noreferrer"
                      target="_blank"
                    >
                      Open review
                    </a>
                  </div>
                </td>
                <td>{scoreText(review.viralScore)}</td>
                <td>{scoreText(review.qualityScore)}</td>
                <td>{reportStatusText(review)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      ) : null}
    </section>
  );
}

function scoreText(value: number | null): string {
  return value === null ? "-" : value.toString();
}

function reportStatusText(review: Review): string {
  if (review.viralScore === null || review.qualityScore === null || review.usefulForReport === null) {
    return "Pending";
  }
  return review.usefulForReport ? "Included" : "Excluded";
}

function reviewSummaryText(review: Review): string {
  const sourceText = review.summary || (review.evidence.length > 0 ? review.evidence.join(" · ") : review.title || review.rawText);
  return truncateText(sourceText);
}

function truncateText(value: string): string {
  const normalized = value.replace(/\s+/g, " ").trim();
  if (normalized.length <= 140) {
    return normalized;
  }
  return `${normalized.slice(0, 137)}...`;
}

function AdminShell({
  activePath,
  fetcher,
  onNavigate,
  session
}: {
  activePath: string;
  fetcher: typeof fetch;
  onNavigate: (path: string) => void;
  session: AuthSession;
}) {
  const page = adminPages[activePath] ?? adminPages["/admin/reviews"];
  const api = useMemo(
    () =>
      createAdminApiClient({
        baseUrl: `${apiBaseUrl}/admin/api/v1`,
        fetcher,
        getAccessToken: () => session.accessToken
      }),
    [fetcher, session.accessToken]
  );
  const [rows, setRows] = useState<ApiRecord[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setIsLoading(true);
    setError(null);

    loadAdminRows(api, activePath)
      .then((result) => {
        if (!cancelled) {
          setRows(result);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setRows([]);
          setError("Unable to load admin data.");
        }
      })
      .finally(() => {
        if (!cancelled) {
          setIsLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [activePath, api]);

  return (
    <div className="appFrame">
      <Header onNavigate={onNavigate} />
      <main className="page">
        <aside className="sidebar">
          <nav aria-label="Admin">
            {adminRoutes.map((route) => (
              <button
                className={activePath === route.path ? "navItem active" : "navItem"}
                key={route.path}
                type="button"
                onClick={() => onNavigate(route.path)}
              >
                {route.label}
              </button>
            ))}
          </nav>
        </aside>
        <section className="content">
          <PageHeader title={page.title} description={page.description} />
          <span className="modeLabel">Read only</span>
          {error ? <p className="errorText">{error}</p> : null}
          {isLoading ? <p>Loading admin data...</p> : <ReadonlyTable page={page} rows={rows} />}
        </section>
      </main>
    </div>
  );
}

function loadAdminRows(
  api: ReturnType<typeof createAdminApiClient>,
  activePath: string
): Promise<ApiRecord[]> {
  switch (activePath) {
    case "/admin/review-targets":
      return api.listReviewTargets();
    case "/admin/collection-runs":
      return api.listCollectionRuns();
    case "/admin/analysis-runs":
      return api.listAnalysisRuns();
    case "/admin/retry-jobs":
      return api.listRetryJobs();
    case "/admin/dead-letters":
      return api.listDeadLetters();
    case "/admin/notifications":
      return api.listNotifications();
    case "/admin/reviews":
    default:
      return api.listReviews();
  }
}

function AccessRequired({ onNavigate }: { onNavigate: (path: string) => void }) {
  return (
    <div className="appFrame">
      <Header onNavigate={onNavigate} />
      <main className="notice">
        <h1>Access required</h1>
        <p>Admin pages require an authenticated administrator session.</p>
        <button type="button" onClick={() => onNavigate("/")}>
          Back to public view
        </button>
      </main>
    </div>
  );
}

function PageHeader({ title, description }: { title: string; description: string }) {
  return (
    <div className="pageHeader">
      <h1>{title}</h1>
      <p>{description}</p>
    </div>
  );
}

function ReadonlyTable({ page, rows }: { page: TablePage; rows: ApiRecord[] }) {
  if (rows.length === 0) {
    return <p>No records found.</p>;
  }

  return (
    <table>
      <thead>
        <tr>
          {page.columns.map((column) => (
            <th key={column}>{column}</th>
          ))}
        </tr>
      </thead>
      <tbody>
        {rows.map((row) => (
          <tr key={String(row.id ?? Object.values(row).join("-"))}>
            {page.columns.map((column) => (
              <td key={column}>{formatCellValue(row[column])}</td>
            ))}
          </tr>
        ))}
      </tbody>
    </table>
  );
}

function formatCellValue(value: ApiRecord[string]): string {
  if (value === null || value === undefined) {
    return "-";
  }
  return String(value);
}
