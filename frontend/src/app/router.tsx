import { Suspense, lazy, type ReactNode } from "react";
import { createBrowserRouter, Navigate } from "react-router-dom";

import { AppLayout } from "@/components/layout/app-layout";
import { AuthLayout } from "@/components/layout/auth-layout";
import { PlatformLayout } from "@/components/layout/platform-layout";
import { ProtectedRoute } from "@/features/auth/protected-route";
import { PublicRoute } from "@/features/auth/public-route";
import { RouteFallback } from "@/i18n/route-fallback";

const LoginPage = lazy(() =>
  import("@/features/auth/login-page").then((module) => ({ default: module.LoginPage })),
);
const ForgotPasswordPage = lazy(() =>
  import("@/features/auth/forgot-password-page").then((module) => ({
    default: module.ForgotPasswordPage,
  })),
);
const ResetPasswordPage = lazy(() =>
  import("@/features/auth/reset-password-page").then((module) => ({
    default: module.ResetPasswordPage,
  })),
);
const VerifyEmailPage = lazy(() =>
  import("@/features/auth/verify-email-page").then((module) => ({
    default: module.VerifyEmailPage,
  })),
);
const ProfilePage = lazy(() =>
  import("@/features/auth/profile-page").then((module) => ({ default: module.ProfilePage })),
);
const WelcomePage = lazy(() =>
  import("@/features/business/welcome-page").then((module) => ({ default: module.WelcomePage })),
);
const BusinessSetupPage = lazy(() =>
  import("@/features/business/business-setup-page").then((module) => ({
    default: module.BusinessSetupPage,
  })),
);
const DashboardPage = lazy(() =>
  import("@/features/dashboard/dashboard-page").then((module) => ({
    default: module.DashboardPage,
  })),
);
const CustomersPage = lazy(() =>
  import("@/features/customers/customers-page").then((module) => ({
    default: module.CustomersPage,
  })),
);
const InventoryPage = lazy(() =>
  import("@/features/inventory/inventory-page").then((module) => ({
    default: module.InventoryPage,
  })),
);
const PurchasesPage = lazy(() =>
  import("@/features/purchases/purchases-page").then((module) => ({
    default: module.PurchasesPage,
  })),
);
const ProductsPage = lazy(() =>
  import("@/features/products/products-page").then((module) => ({
    default: module.ProductsPage,
  })),
);
const ReportsPage = lazy(() =>
  import("@/features/reports/reports-page").then((module) => ({ default: module.ReportsPage })),
);
const SalesPage = lazy(() =>
  import("@/features/sales/sales-page").then((module) => ({ default: module.SalesPage })),
);
const SettingsPage = lazy(() =>
  import("@/features/settings/settings-page").then((module) => ({
    default: module.SettingsPage,
  })),
);
const SuppliersPage = lazy(() =>
  import("@/features/suppliers/suppliers-page").then((module) => ({
    default: module.SuppliersPage,
  })),
);
const AuditPage = lazy(() =>
  import("@/features/audit/audit-page").then((module) => ({ default: module.AuditPage })),
);
const PosPage = lazy(() =>
  import("@/features/pos/pos-page").then((module) => ({ default: module.PosPage })),
);
const TeamPage = lazy(() =>
  import("@/features/staff/team-page").then((module) => ({ default: module.TeamPage })),
);
const AcceptInvitePage = lazy(() =>
  import("@/features/staff/accept-invite-page").then((module) => ({
    default: module.AcceptInvitePage,
  })),
);
const PlatformDashboardPage = lazy(() =>
  import("@/features/platform/platform-dashboard-page").then((module) => ({
    default: module.PlatformDashboardPage,
  })),
);
const PlatformShopsPage = lazy(() =>
  import("@/features/platform/platform-shops-page").then((module) => ({
    default: module.PlatformShopsPage,
  })),
);
const PlatformAddShopPage = lazy(() =>
  import("@/features/platform/platform-add-shop-page").then((module) => ({
    default: module.PlatformAddShopPage,
  })),
);
const PlatformShopDetailPage = lazy(() =>
  import("@/features/platform/platform-shop-detail-page").then((module) => ({
    default: module.PlatformShopDetailPage,
  })),
);
const PlatformUsersPage = lazy(() =>
  import("@/features/platform/platform-users-page").then((module) => ({
    default: module.PlatformUsersPage,
  })),
);
const PlatformSettingsPage = lazy(() =>
  import("@/features/platform/platform-settings-page").then((module) => ({
    default: module.PlatformSettingsPage,
  })),
);

function withSuspense(element: ReactNode) {
  return <Suspense fallback={<RouteFallback />}>{element}</Suspense>;
}

export const router = createBrowserRouter([
  {
    element: <PublicRoute />,
    children: [
      {
        path: "/login",
        element: withSuspense(
          <AuthLayout>
            <LoginPage />
          </AuthLayout>,
        ),
      },
      {
        path: "/forgot-password",
        element: withSuspense(
          <AuthLayout>
            <ForgotPasswordPage />
          </AuthLayout>,
        ),
      },
    ],
  },
  {
    children: [
      {
        path: "/reset-password",
        element: withSuspense(
          <AuthLayout>
            <ResetPasswordPage />
          </AuthLayout>,
        ),
      },
      {
        path: "/verify-email",
        element: withSuspense(
          <AuthLayout>
            <VerifyEmailPage />
          </AuthLayout>,
        ),
      },
      {
        path: "/accept-invite",
        element: withSuspense(
          <AuthLayout>
            <AcceptInvitePage />
          </AuthLayout>,
        ),
      },
    ],
  },
  {
    element: <ProtectedRoute />,
    children: [
      {
        element: <PlatformLayout />,
        children: [
          { path: "/platform", element: withSuspense(<PlatformDashboardPage />) },
          { path: "/platform/shops", element: withSuspense(<PlatformShopsPage />) },
          { path: "/platform/shops/new", element: withSuspense(<PlatformAddShopPage />) },
          { path: "/platform/shops/:id", element: withSuspense(<PlatformShopDetailPage />) },
          { path: "/platform/users", element: withSuspense(<PlatformUsersPage />) },
          { path: "/platform/settings", element: withSuspense(<PlatformSettingsPage />) },
        ],
      },
      {
        element: <AppLayout />,
        children: [
          {
            index: true,
            element: <Navigate to="/pos" replace />,
          },
          { path: "/welcome", element: withSuspense(<WelcomePage />) },
          { path: "/dashboard", element: withSuspense(<DashboardPage />) },
          { path: "/business-setup", element: withSuspense(<BusinessSetupPage />) },
          { path: "/profile", element: withSuspense(<ProfilePage />) },
          { path: "/products", element: withSuspense(<ProductsPage />) },
          { path: "/inventory", element: withSuspense(<InventoryPage />) },
          { path: "/suppliers", element: withSuspense(<SuppliersPage />) },
          { path: "/customers", element: withSuspense(<CustomersPage />) },
          { path: "/purchases", element: withSuspense(<PurchasesPage />) },
          { path: "/sales", element: withSuspense(<SalesPage />) },
          { path: "/pos", element: withSuspense(<PosPage />) },
          { path: "/team", element: withSuspense(<TeamPage />) },
          { path: "/audit", element: withSuspense(<AuditPage />) },
          { path: "/reports", element: withSuspense(<ReportsPage />) },
          { path: "/settings", element: withSuspense(<SettingsPage />) },
        ],
      },
    ],
  },
  {
    path: "*",
    element: <Navigate to="/pos" replace />,
  },
]);
