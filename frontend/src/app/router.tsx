import { createBrowserRouter, Navigate } from "react-router-dom";

import { AppLayout } from "@/components/layout/app-layout";
import { AuthLayout } from "@/components/layout/auth-layout";
import { DashboardPage } from "@/features/dashboard/dashboard-page";
import { ForgotPasswordPage } from "@/features/auth/forgot-password-page";
import { LoginPage } from "@/features/auth/login-page";
import { ProfilePage } from "@/features/auth/profile-page";
import { ProtectedRoute } from "@/features/auth/protected-route";
import { PublicRoute } from "@/features/auth/public-route";
import { ResetPasswordPage } from "@/features/auth/reset-password-page";
import { VerifyEmailPage } from "@/features/auth/verify-email-page";
import { BusinessSetupPage } from "@/features/business/business-setup-page";
import { CustomersPage } from "@/features/customers/customers-page";
import { InventoryPage } from "@/features/inventory/inventory-page";
import { PurchasesPage } from "@/features/purchases/purchases-page";
import { ProductsPage } from "@/features/products/products-page";
import { ReportsPage } from "@/features/reports/reports-page";
import { SalesPage } from "@/features/sales/sales-page";
import { SettingsPage } from "@/features/settings/settings-page";
import { SuppliersPage } from "@/features/suppliers/suppliers-page";

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
    ],
  },
  {
    element: <ProtectedRoute />,
    children: [
      {
        element: <AppLayout />,
        children: [
          {
            index: true,
            element: <Navigate to="/dashboard" replace />,
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
    element: <Navigate to="/dashboard" replace />,
  },
]);
