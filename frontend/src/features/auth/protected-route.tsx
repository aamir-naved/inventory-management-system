import { Navigate, Outlet, useLocation } from "react-router-dom";

import { useAuth } from "@/features/auth/auth-context";
import { canOpenPath } from "@/features/auth/roles";
import { afterAuthPath, isPlatformAdmin } from "@/features/auth/auth-storage";

export function ProtectedRoute() {
  const { isAuthenticated, session } = useAuth();
  const location = useLocation();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }

  const path = location.pathname;
  const platform = isPlatformAdmin(session);

  if (platform && !path.startsWith("/platform")) {
    return <Navigate to="/platform" replace />;
  }

  if (!platform && path.startsWith("/platform")) {
    return <Navigate to={afterAuthPath(session)} replace />;
  }

  if (!platform) {
    const needsShop =
      !session?.businessId &&
      !path.startsWith("/welcome") &&
      !path.startsWith("/business-setup") &&
      !path.startsWith("/profile");

    if (needsShop) {
      return <Navigate to="/welcome" replace />;
    }

    if (!canOpenPath(session?.role, path)) {
      return <Navigate to="/pos" replace />;
    }
  }

  return <Outlet />;
}
