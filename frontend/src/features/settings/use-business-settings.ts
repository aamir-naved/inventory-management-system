import { useQuery } from "@tanstack/react-query";

import { useAuth } from "@/features/auth/auth-context";
import {
  formatDate,
  formatDateTime,
  formatMoney,
} from "@/features/settings/format";
import { getSettings } from "@/features/settings/settings-api";

export function useBusinessSettings() {
  const { session } = useAuth();
  const businessId = session?.businessId ?? null;

  const settingsQuery = useQuery({
    queryKey: ["settings", businessId],
    queryFn: () => getSettings(businessId!),
    enabled: Boolean(businessId),
  });

  const currencyCode = settingsQuery.data?.currencyCode ?? "INR";
  const dateFormat = settingsQuery.data?.dateFormat ?? "dd/MM/yyyy";
  const defaultLowStockThreshold = Number(
    settingsQuery.data?.defaultLowStockThreshold ?? 0,
  );
  const allowNegativeStock = settingsQuery.data?.allowNegativeStock ?? false;

  return {
    businessId,
    settings: settingsQuery.data,
    isLoading: settingsQuery.isLoading,
    currencyCode,
    dateFormat,
    defaultLowStockThreshold,
    allowNegativeStock,
    formatMoney: (value: number | undefined | null) =>
      formatMoney(value, currencyCode),
    formatDate: (value: string | Date | null | undefined) =>
      formatDate(value, dateFormat),
    formatDateTime: (value: string | Date | null | undefined) =>
      formatDateTime(value, dateFormat),
  };
}
