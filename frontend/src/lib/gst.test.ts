import { describe, expect, it } from "vitest";

import { computeGstLine } from "@/lib/gst";
import { canManageCatalog, canOpenPath, normalizeRole } from "@/features/auth/roles";
import { afterAuthPath, type SessionUser } from "@/features/auth/auth-storage";

describe("computeGstLine", () => {
  it("adds 18% GST on exclusive intra-state sales", () => {
    const line = computeGstLine(2, 100, 18, false, false);
    expect(line.taxableAmount).toBe(200);
    expect(line.cgstAmount).toBe(18);
    expect(line.sgstAmount).toBe(18);
    expect(line.lineTotal).toBe(236);
  });

  it("uses IGST for interstate sales", () => {
    const line = computeGstLine(1, 100, 18, false, true);
    expect(line.igstAmount).toBe(18);
    expect(line.cgstAmount).toBe(0);
    expect(line.lineTotal).toBe(118);
  });
});

describe("roles", () => {
  it("treats clerks as sales-only", () => {
    expect(normalizeRole("CLERK")).toBe("CLERK");
    expect(canManageCatalog("CLERK")).toBe(false);
    expect(canOpenPath("CLERK", "/pos")).toBe(true);
    expect(canOpenPath("CLERK", "/welcome")).toBe(true);
    expect(canOpenPath("CLERK", "/reports")).toBe(false);
  });
});

describe("afterAuthPath", () => {
  const baseUser: SessionUser = {
    userId: "u1",
    fullName: "Aamir",
    email: "a@example.com",
    phone: null,
    emailVerified: true,
    businessId: null,
    businessName: null,
    role: null,
    platformRole: null,
  };

  it("sends platform admins to the console", () => {
    expect(afterAuthPath({ ...baseUser, platformRole: "PLATFORM_ADMIN" })).toBe("/platform");
  });

  it("sends shop members to the counter", () => {
    expect(afterAuthPath({ ...baseUser, businessId: "b1", role: "OWNER" })).toBe("/pos");
  });

  it("sends users without a shop to start", () => {
    expect(afterAuthPath(baseUser)).toBe("/welcome");
  });
});
