import { describe, expect, it } from "vitest";

import { translate } from "@/i18n/messages";
import { formatMoney } from "@/features/settings/format";

describe("translate", () => {
  it("returns Hindi nav labels", () => {
    expect(translate("hi", "nav.counter")).toBe("काउंटर");
    expect(translate("hi", "pos.completeSale")).toBe("बिक्री पूरी करें");
  });

  it("interpolates variables", () => {
    expect(translate("en", "shell.welcome", { name: "Aamir" })).toBe("Welcome back, Aamir");
  });
});

describe("formatMoney", () => {
  it("uses en-IN grouping for INR", () => {
    const formatted = formatMoney(123456.78, "INR");
    expect(formatted).toContain("1,23,456.78");
  });
});
