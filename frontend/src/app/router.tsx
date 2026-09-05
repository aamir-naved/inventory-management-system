import { createBrowserRouter, Navigate } from "react-router-dom";

import { AppLayout } from "@/components/layout/app-layout";
import { AuthLayout } from "@/components/layout/auth-layout";
import { PlatformLayout } from "@/components/layout/platform-layout";
import { DashboardPage } from "@/features/dashboard/dashboard-page";
import { ForgotPasswordPage } from "@/features/auth/forgot-password-page";
import { LoginPage } from "@/features/auth/login-page";
import { ProfilePage } from "@/features/auth/profile-page";
import { ProtectedRoute } from "@/features/auth/protected-route";
import { PublicRoute } from "@/features/auth/public-route";
import { ResetPasswordPage } from "@/features/auth/reset-password-page";
import { VerifyEmailPage } from "@/features/auth/verify-email-page";
import { BusinessSetupPage } from "@/features/business/business-setup-page";
import { WelcomePage } from "@/features/business/welcome-page";
import { CustomersPage } from "@/features/customers/customers-page";
import { InventoryPage } from "@/features/inventory/inventory-page";
import { PurchasesPage } from "@/features/purchases/purchases-page";
import { ProductsPage } from "@/features/products/products-page";
import { ReportsPage } from "@/features/reports/reports-page";
import { SalesPage } from "@/features/sales/sales-page";
import { SettingsPage } from "@/features/settings/settings-page";
import { SuppliersPage } from "@/features/suppliers/suppliers-page";
import { AuditPage } from "@/features/audit/audit-page";
import { PosPage } from "@/features/pos/pos-page";
import { TeamPage } from "@/features/staff/team-page";
import { AcceptInvitePage } from "@/features/staff/accept-invite-page";
import { PlatformDashboardPage } from "@/features/platform/platform-dashboard-page";
import { PlatformShopsPage } from "@/features/platform/platform-shops-page";
import { PlatformAddShopPage } from "@/features/platform/platform-add-shop-page";
import { PlatformShopDetailPage } from "@/features/platform/platform-shop-detail-page";
import { PlatformUsersPage } from "@/features/platform/platform-users-page";
import { PlatformSettingsPage } from "@/features/platform/platform-settings-page";

export const router = createBrowserRouter([
  {
    element: <PublicRoute />,
    children: [
      {
        path: "/login",
        element: (
          <AuthLayout>
            <LoginPage />
          </AuthLayout>
        ),
      },
      {
        path: "/forgot-password",
        element: (
          <AuthLayout>
            <ForgotPasswordPage />
          </AuthLayout>
        ),
      },
    ],
  },
  {
    children: [
      {
        path: "/reset-password",
        element: (
          <AuthLayout>
            <ResetPasswordPage />
          </AuthLayout>
        ),
      },
      {
        path: "/verify-email",
        element: (
          <AuthLayout>
            <VerifyEmailPage />
          </AuthLayout>
        ),
      },
      {
        path: "/accept-invite",
        element: (
          <AuthLayout>
            <AcceptInvitePage />
          </AuthLayout>
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
          { path: "/platform", element: <PlatformDashboardPage /> },
          { path: "/platform/shops", element: <PlatformShopsPage /> },
          { path: "/platform/shops/new", element: <PlatformAddShopPage /> },
          { path: "/platform/shops/:id", element: <PlatformShopDetailPage /> },
          { path: "/platform/users", element: <PlatformUsersPage /> },
          { path: "/platform/settings", element: <PlatformSettingsPage /> },
        ],
      },
      {
        element: <AppLayout />,
        children: [
          {
            index: true,
            element: <Navigate to="/pos" replace />,
          },
          {
            path: "/welcome",
            element: <WelcomePage />,
          },
          {
            path: "/dashboard",
            element: <DashboardPage />,
          },
          {
            path: "/business-setup",
            element: <BusinessSetupPage />,
          },
          {
            path: "/profile",
            element: <ProfilePage />,
          },
          {
            path: "/products",
            element: <ProductsPage />,
          },
          {
            path: "/inventory",
            element: <InventoryPage />,
          },
          {
            path: "/suppliers",
            element: <SuppliersPage />,
          },
          {
            path: "/customers",
            element: <CustomersPage />,
          },
          {
            path: "/purchases",
            element: <PurchasesPage />,
          },
          {
            path: "/sales",
            element: <SalesPage />,
          },
          {
            path: "/pos",
            element: <PosPage />,
          },
          {
            path: "/team",
            element: <TeamPage />,
          },
          {
            path: "/audit",
            element: <AuditPage />,
          },
          {
            path: "/reports",
            element: <ReportsPage />,
          },
          {
            path: "/settings",
            element: <SettingsPage />,
          },
        ],
      },
    ],
  },
  {
    path: "*",
    element: <Navigate to="/pos" replace />,
  },
]);
