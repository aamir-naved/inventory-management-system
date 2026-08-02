import { createBrowserRouter, Navigate } from "react-router-dom";

import { AppLayout } from "@/components/layout/app-layout";
import { AuthLayout } from "@/components/layout/auth-layout";
import { DashboardPage } from "@/features/dashboard/dashboard-page";
import { LoginPage } from "@/features/auth/login-page";
import { ProtectedRoute } from "@/features/auth/protected-route";
import { PublicRoute } from "@/features/auth/public-route";
import { BusinessSetupPage } from "@/features/business/business-setup-page";
import { InventoryPage } from "@/features/inventory/inventory-page";
import { PurchasesPage } from "@/features/purchases/purchases-page";
import { ProductsPage } from "@/features/products/products-page";
import { SalesPage } from "@/features/sales/sales-page";

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
            path: "/products",
            element: <ProductsPage />,
          },
          {
            path: "/inventory",
            element: <InventoryPage />,
          },
          {
            path: "/purchases",
            element: <PurchasesPage />,
          },
          {
            path: "/sales",
            element: <SalesPage />,
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
