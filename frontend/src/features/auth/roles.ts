export type MembershipRole = "OWNER" | "MANAGER" | "CLERK";

export function normalizeRole(role: string | null | undefined): MembershipRole {
  if (role === "MANAGER" || role === "CLERK") {
    return role;
  }
  return "OWNER";
}

export function canManageStaff(role: string | null | undefined) {
  return normalizeRole(role) === "OWNER";
}

export function canManageCatalog(role: string | null | undefined) {
  return normalizeRole(role) !== "CLERK";
}

export function canViewReports(role: string | null | undefined) {
  return normalizeRole(role) !== "CLERK";
}

export function canAdjustStock(role: string | null | undefined) {
  return normalizeRole(role) !== "CLERK";
}

export function canOpenPath(role: string | null | undefined, path: string) {
  if (canManageCatalog(role)) {
    return true;
  }
  return (
    path.startsWith("/dashboard") ||
    path.startsWith("/welcome") ||
    path.startsWith("/profile") ||
    path.startsWith("/products") ||
    path.startsWith("/inventory") ||
    path.startsWith("/customers") ||
    path.startsWith("/sales") ||
    path.startsWith("/pos")
  );
}
