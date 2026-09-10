import { expect, test, type Page } from "@playwright/test";

async function registerAndOpenShop(page: Page) {
  const stamp = Date.now();
  const email = `e2e-${stamp}@example.com`;
  const password = "Password123!";
  const shopName = `E2E Shop ${stamp}`;

  await page.goto("/login");
  await page.waitForLoadState("networkidle");

  const signup = page.getByRole("button", { name: "Email signup" });
  await expect(signup, "Public registration must be open for this E2E (APP_OPEN_REGISTRATION=true)").toBeVisible({
    timeout: 15_000,
  });
  await signup.click();
  await expect(page.getByLabel("Full name")).toBeVisible();
  await page.getByLabel("Full name").fill("E2E Owner");
  await page.locator("#email").fill(email);
  await page.locator("#password").fill(password);
  await page.getByRole("button", { name: "Create account" }).click();

  await expect(page.getByLabel("Shop name")).toBeVisible({ timeout: 30_000 });
  await page.getByLabel("Shop name").fill(shopName);
  await page.getByLabel("Shop mobile").fill("9876543210");
  await page.getByRole("button", { name: "Open the counter" }).click();

  await expect(page.getByRole("heading", { name: /Fast sale entry/i })).toBeVisible({
    timeout: 30_000,
  });

  return { email, password, shopName };
}

test.describe("shop counter loop", () => {
  test("login → counter sale with payment → stock drop → share bill", async ({ page }) => {
    await registerAndOpenShop(page);

    const hint = page.getByRole("button", { name: "Got it" });
    if (await hint.isVisible().catch(() => false)) {
      await hint.click();
    }

    await page.getByLabel("Barcode").fill("CEM-001");
    await page.getByLabel("Barcode").press("Enter");
    await expect(page.getByText("Cement")).toBeVisible();

    // Cement starter sell price is 380; amount paid blank → full payment.
    await page.getByRole("button", { name: "Complete sale" }).click();
    await expect(page.getByText(/Saved .+\. Share the bill with the customer\./)).toBeVisible({
      timeout: 30_000,
    });

    const invoiceResponsePromise = page.waitForResponse(
      (response) =>
        response.url().includes("/invoice.pdf") &&
        response.request().method() === "GET" &&
        response.ok(),
      { timeout: 30_000 },
    );

    // Headless Chromium has no Web Share; share falls back to PDF download + WhatsApp.
    page.on("dialog", (dialog) => dialog.dismiss().catch(() => undefined));
    await page.getByRole("button", { name: "Share bill" }).click();
    const invoiceResponse = await invoiceResponsePromise;
    expect(invoiceResponse.status()).toBe(200);
    const contentType = invoiceResponse.headers()["content-type"] ?? "";
    expect(contentType).toMatch(/pdf|octet-stream/i);

    await page.getByRole("link", { name: "Inventory" }).click();
    await expect(
      page.getByRole("heading", {
        name: /Track stock levels, value, low-stock risk, and every manual adjustment/i,
      }),
    ).toBeVisible({ timeout: 30_000 });
    await expect(page.getByText("Cement").first()).toBeVisible();
    // Starter opening stock is 20; one bag sold → 19.000
    await expect(page.getByText(/Current stock:\s*19\.000/)).toBeVisible({ timeout: 30_000 });
  });
});
