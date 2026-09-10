import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { PosPage } from "@/features/pos/pos-page";

const listCustomers = vi.fn();
const getProductByBarcode = vi.fn();
const createSale = vi.fn();
const shareSaleInvoice = vi.fn();
const printSaleInvoice = vi.fn();

vi.mock("@/features/auth/auth-context", () => ({
  useAuth: () => ({
    session: {
      userId: "user-1",
      fullName: "Owner",
      email: "owner@example.com",
      phone: null,
      emailVerified: true,
      businessId: "biz-1",
      businessName: "Test Shop",
      role: "OWNER",
      platformRole: null,
    },
  }),
}));

vi.mock("@/features/customers/customer-api", () => ({
  listCustomers: (...args: unknown[]) => listCustomers(...args),
}));

vi.mock("@/features/products/product-api", () => ({
  getProductByBarcode: (...args: unknown[]) => getProductByBarcode(...args),
}));

vi.mock("@/features/sales/sales-api", () => ({
  createSale: (...args: unknown[]) => createSale(...args),
}));

vi.mock("@/features/documents/document-api", () => ({
  shareSaleInvoice: (...args: unknown[]) => shareSaleInvoice(...args),
  printSaleInvoice: (...args: unknown[]) => printSaleInvoice(...args),
}));

import { LocaleProvider } from "@/i18n/locale-context";

vi.mock("@/features/settings/use-business-settings", () => ({
  useBusinessSettings: () => ({
    formatMoney: (value: number) => `₹${Number(value).toFixed(2)}`,
    gstEnabled: false,
    gstInclusivePricing: false,
  }),
}));

function renderPos() {
  const client = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });
  return render(
    <QueryClientProvider client={client}>
      <LocaleProvider>
        <MemoryRouter>
          <PosPage />
        </MemoryRouter>
      </LocaleProvider>
    </QueryClientProvider>,
  );
}

describe("PosPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    try {
      window.localStorage.clear();
    } catch {
      // jsdom / node may not expose localStorage in every environment.
    }
    listCustomers.mockResolvedValue({
      items: [
        {
          id: "cust-walkin",
          businessId: "biz-1",
          name: "Walk-in",
          contactPerson: null,
          mobileNumber: null,
          addressLine: null,
          archived: false,
          createdAt: "2026-01-01T00:00:00Z",
          updatedAt: "2026-01-01T00:00:00Z",
        },
      ],
      page: 0,
      size: 100,
      totalItems: 1,
      totalPages: 1,
    });
    getProductByBarcode.mockResolvedValue({
      id: "prod-cement",
      businessId: "biz-1",
      name: "Cement",
      sku: "CEM-001",
      category: "Cement",
      unit: "Bags",
      costPrice: 320,
      sellingPrice: 380,
      openingStock: 20,
      currentStock: 20,
      lowStockThreshold: 5,
      archived: false,
      barcode: "CEM-001",
      hsnCode: "2523",
      gstRate: 28,
      createdAt: "2026-01-01T00:00:00Z",
      updatedAt: "2026-01-01T00:00:00Z",
    });
    createSale.mockResolvedValue({
      id: "sale-1",
      saleNumber: "SAL/2025-26/000001",
    });
    shareSaleInvoice.mockResolvedValue("downloaded");
    printSaleInvoice.mockResolvedValue(undefined);
  });

  it("auto-selects Walk-in, adds a barcode line, completes sale with payment, and shares the bill", async () => {
    const user = userEvent.setup();
    renderPos();

    await waitFor(() => {
      expect(screen.getByLabelText("Customer")).toHaveValue("cust-walkin");
    });

    await user.type(screen.getByLabelText("Barcode"), "CEM-001");
    await user.keyboard("{Enter}");

    expect(await screen.findByText("Cement")).toBeInTheDocument();
    expect(screen.getByText(/Total ₹380.00/)).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Complete sale" }));

    await waitFor(() => {
      expect(createSale).toHaveBeenCalledWith(
        "biz-1",
        expect.objectContaining({
          customerId: "cust-walkin",
          amountPaid: 380,
          notes: "POS sale",
          items: [
            expect.objectContaining({
              productId: "prod-cement",
              quantity: 1,
              sellingPrice: 380,
            }),
          ],
        }),
        expect.any(String),
      );
    });

    expect(
      await screen.findByText(/Saved SAL\/2025-26\/000001\. Share the bill with the customer\./),
    ).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Share bill" }));

    await waitFor(() => {
      expect(shareSaleInvoice).toHaveBeenCalledWith(
        "biz-1",
        "sale-1",
        "SAL/2025-26/000001",
      );
    });
    expect(
      await screen.findByText(/Saved SAL\/2025-26\/000001\. Attach the PDF in WhatsApp\./),
    ).toBeInTheDocument();
  });

  it("removes a scanned line before checkout", async () => {
    const user = userEvent.setup();
    renderPos();

    await user.type(screen.getByLabelText("Barcode"), "CEM-001");
    await user.keyboard("{Enter}");
    expect(await screen.findByText("Cement")).toBeInTheDocument();

    const row = screen.getByText("Cement").closest("li");
    expect(row).toBeTruthy();
    await user.click(within(row as HTMLElement).getByRole("button", { name: "Remove" }));

    expect(screen.queryByText("Cement")).not.toBeInTheDocument();
  });
});
