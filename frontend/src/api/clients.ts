type Fetcher = typeof fetch;

type ClientOptions = {
  baseUrl: string;
  fetcher?: Fetcher;
  getAccessToken?: () => string | null;
};

type AdminClientOptions = ClientOptions & {
  getAccessToken: () => string | null;
};

export type ApiRecord = Record<string, string | number | boolean | null | undefined>;

export type ReviewTargetType = "PLACE" | "PRODUCT";

export type ReviewTarget = {
  id: string;
  createdBy?: string;
  type: ReviewTargetType;
  keyword: string;
  status: string;
};

export type Review = {
  id: string;
  targetId: string;
  source: string;
  sourceReviewId: string | null;
  canonicalUrl: string;
  title: string | null;
  rawText: string;
  summary: string | null;
  publishedAt: string | null;
  status: string;
  viralScore: number | null;
  qualityScore: number | null;
  isSuspicious: boolean | null;
  usefulForReport: boolean | null;
  detectedPatterns: string[];
  evidence: string[];
};

export type CollectionRun = {
  id: string;
  targetId: string;
  source: string;
  keyword: string;
  status: string;
  runReason: string;
  windowFrom: string;
  windowTo: string;
  maxReviews: number;
};

async function readJson<T>(response: Response): Promise<T> {
  if (!response.ok) {
    throw new Error(`Request failed with ${response.status}`);
  }

  return (await response.json()) as T;
}

function joinUrl(baseUrl: string, path: string) {
  return `${baseUrl.replace(/\/$/, "")}/${path.replace(/^\//, "")}`;
}

function authHeaders(getAccessToken?: () => string | null): Record<string, string> {
  const token = getAccessToken?.();
  return token ? { Authorization: `Bearer ${token}` } : {};
}

function publicGet<T>(
  baseUrl: string,
  fetcher: Fetcher,
  path: string,
  getAccessToken?: () => string | null
) {
  return fetcher(joinUrl(baseUrl, path), {
    headers: {
      Accept: "application/json",
      ...authHeaders(getAccessToken)
    }
  }).then((response) => readJson<T>(response));
}

function publicPost<T>(
  baseUrl: string,
  fetcher: Fetcher,
  path: string,
  body: unknown,
  getAccessToken?: () => string | null
) {
  return fetcher(joinUrl(baseUrl, path), {
    method: "POST",
    headers: {
      Accept: "application/json",
      "Content-Type": "application/json",
      ...authHeaders(getAccessToken)
    },
    body: JSON.stringify(body)
  }).then((response) => readJson<T>(response));
}

export type CreateReviewTargetPayload = {
  type: ReviewTargetType;
  keyword: string;
};

function adminGet<T>(
  baseUrl: string,
  fetcher: Fetcher,
  getAccessToken: () => string | null,
  path: string
) {
  const token = getAccessToken();
  return fetcher(joinUrl(baseUrl, path), {
    headers: {
      Accept: "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {})
    }
  }).then((response) => readJson<T>(response));
}

export function createPublicApiClient({
  baseUrl,
  fetcher = fetch,
  getAccessToken
}: ClientOptions) {
  return {
    listReviewTargets() {
      return publicGet<ReviewTarget[]>(baseUrl, fetcher, "/review-targets", getAccessToken);
    },
    createReviewTarget(payload: CreateReviewTargetPayload) {
      return publicPost<ReviewTarget>(
        baseUrl,
        fetcher,
        "/review-targets",
        payload,
        getAccessToken
      );
    },
    getLatestReviewReport(reviewTargetId: string) {
      return publicGet<ApiRecord>(
        baseUrl,
        fetcher,
        `/review-targets/${reviewTargetId}/report`,
        getAccessToken
      );
    },
    listReviewTargetReviews(reviewTargetId: string) {
      return publicGet<Review[]>(
        baseUrl,
        fetcher,
        `/review-targets/${reviewTargetId}/reviews`,
        getAccessToken
      );
    },
    requestReviewTargetCollection(reviewTargetId: string) {
      return publicPost<CollectionRun>(
        baseUrl,
        fetcher,
        `/review-targets/${reviewTargetId}/collection-runs`,
        {},
        getAccessToken
      );
    }
  };
}

export function createAdminApiClient({
  baseUrl,
  fetcher = fetch,
  getAccessToken
}: AdminClientOptions) {
  const list = (path: string) =>
    adminGet<ApiRecord[]>(baseUrl, fetcher, getAccessToken, path);

  return {
    listReviewTargets: () => list("/review-targets"),
    listReviews: () => list("/reviews"),
    listCollectionRuns: () => list("/collection-runs"),
    listAnalysisRuns: () => list("/analysis-runs"),
    listRetryJobs: () => list("/retry-jobs"),
    listDeadLetters: () => list("/dead-letters"),
    listNotifications: () => list("/notification-deliveries")
  };
}
