import { describe, expect, it } from "vitest";

import { newIdempotencyKey } from "@/lib/idempotency";

describe("newIdempotencyKey", () => {
  it("returns a non-empty key", () => {
    const key = newIdempotencyKey();
    expect(key.length).toBeGreaterThan(8);
  });
});
