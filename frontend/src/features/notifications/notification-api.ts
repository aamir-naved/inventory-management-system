import { httpClient } from "@/api/http-client";

export type NotificationItem = {
  type: string;
  title: string;
  detail: string;
  entityId: string | null;
};

export async function listNotifications(businessId: string) {
  return httpClient<{ count: number; items: NotificationItem[] }>("/notifications", {
    businessId,
  });
}
