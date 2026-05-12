import { describe, expect, it } from "vitest";

import { canAccessRoute, getRouteForPath } from "./routes";
import type { UserRole } from "../auth/auth";

describe("route access", () => {
  it("resolves public and admin route shells", () => {
    expect(getRouteForPath("/")).toMatchObject({ shell: "public" });
    expect(getRouteForPath("/admin/reviews")).toMatchObject({
      shell: "admin",
      requiredRole: "ADMIN"
    });
  });

  it.each<[UserRole | null, string, boolean]>([
    [null, "/", true],
    ["USER", "/admin/reviews", false],
    ["ADMIN", "/admin/reviews", true],
    ["USER", "/public/review-targets", true]
  ])("checks %s access to %s", (role, path, expected) => {
    expect(canAccessRoute(role, path)).toBe(expected);
  });
});
