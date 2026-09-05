import { httpClient } from "@/api/http-client";
import { withPaging, type PageRequest, type PagedResult } from "@/api/paging";

export type AuditEvent = {
  id: string;
  createdAt: string;
  userId: string | null;
  action: string;
  entityType: string;
  entityId: string | null;
  summary: string;
};

export async function listAuditEvents(businessId: string, paging: PageRequest = {}) {
  const params = withPaging(new URLSearchParams(), paging);
  return httpClient<PagedResult<AuditEvent>>(`/audit?${params.toString()}`, {
    businessId,
  });
}
