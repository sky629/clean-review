import type { UserRole } from "../auth/auth";

export type RouteShell = "public" | "admin";

export type AppRoute = {
  path: string;
  label: string;
  shell: RouteShell;
  requiredRole?: UserRole;
};

export const publicRoutes: AppRoute[] = [
  { path: "/", label: "Review targets", shell: "public" },
  { path: "/public/review-targets", label: "Review targets", shell: "public" }
];

export const adminRoutes: AppRoute[] = [
  { path: "/admin/review-targets", label: "ReviewTargets", shell: "admin", requiredRole: "ADMIN" },
  { path: "/admin/reviews", label: "Reviews", shell: "admin", requiredRole: "ADMIN" },
  { path: "/admin/collection-runs", label: "CollectionRuns", shell: "admin", requiredRole: "ADMIN" },
  { path: "/admin/analysis-runs", label: "AnalysisRuns", shell: "admin", requiredRole: "ADMIN" },
  { path: "/admin/retry-jobs", label: "RetryJobs", shell: "admin", requiredRole: "ADMIN" },
  { path: "/admin/dead-letters", label: "DeadLetters", shell: "admin", requiredRole: "ADMIN" },
  { path: "/admin/notifications", label: "Notifications", shell: "admin", requiredRole: "ADMIN" }
];

const fallbackRoute = publicRoutes[0];

export function getRouteForPath(path: string): AppRoute {
  return (
    [...publicRoutes, ...adminRoutes].find((route) => route.path === path) ??
    fallbackRoute
  );
}

export function canAccessRoute(role: UserRole | null, path: string) {
  const route = getRouteForPath(path);
  return !route.requiredRole || route.requiredRole === role;
}
