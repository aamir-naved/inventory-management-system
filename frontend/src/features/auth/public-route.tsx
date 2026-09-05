import { Navigate, Outlet } from "react-router-dom";

import { useAuth } from "@/features/auth/auth-context";
import { afterAuthPath } from "@/features/auth/auth-storage";

export function PublicRoute() {
  const { isAuthenticated, session } = useAuth();

  if (isAuthenticated) {
    return <Navigate to={afterAuthPath(session)} replace />;
  }

  return <Outlet />;
}
