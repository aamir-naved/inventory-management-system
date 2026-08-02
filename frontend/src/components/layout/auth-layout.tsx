import type { PropsWithChildren } from "react";

export function AuthLayout({ children }: PropsWithChildren) {
  return <div className="auth-page">{children}</div>;
}
