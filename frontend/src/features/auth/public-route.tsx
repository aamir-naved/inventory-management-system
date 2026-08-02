import { Navigate, Outlet } from "react-router-dom";

import { useAuth } from "@/features/auth/auth-context";

export function PublicRoute() {
  const { isAuthenticated } = useAuth();

  if (isAuthenticated) {
    return <Navigate to="/dashboard" replace />;
  }

  return <Outlet />;
}
