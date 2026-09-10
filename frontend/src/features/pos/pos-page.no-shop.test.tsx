import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";

import { PosPage } from "@/features/pos/pos-page";

vi.mock("@/features/auth/auth-context", () => ({
  useAuth: () => ({
    session: {
      userId: "user-1",
      fullName: "Owner",
      email: "owner@example.com",
      phone: null,
      emailVerified: true,
      businessId: null,
      businessName: null,
      role: "OWNER",
      platformRole: null,
    },
  }),
}));

import { LocaleProvider } from "@/i18n/locale-context";

vi.mock("@/features/settings/use-business-settings", () => ({
  useBusinessSettings: () => ({
    formatMoney: (value: number) => String(value),
    gstEnabled: false,
    gstInclusivePricing: false,
  }),
}));

describe("PosPage without a shop", () => {
  it("asks the clerk to name the shop first", () => {
    const client = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });
    render(
      <QueryClientProvider client={client}>
        <LocaleProvider>
          <MemoryRouter>
            <PosPage />
          </MemoryRouter>
        </LocaleProvider>
      </QueryClientProvider>,
    );

    expect(screen.getByText(/Name the shop before using the counter/i)).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Open the shop" })).toHaveAttribute(
      "href",
      "/welcome",
    );
  });
});
