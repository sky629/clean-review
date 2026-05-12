import { readFileSync } from "node:fs";
import { resolve } from "node:path";

import { describe, expect, it } from "vitest";

describe("document shell", () => {
  it("declares an inline favicon to avoid a browser 404", () => {
    const html = readFileSync(resolve(__dirname, "../index.html"), "utf8");

    expect(html).toContain('rel="icon"');
  });
});
