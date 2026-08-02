import { httpClient } from "@/api/http-client";

export type DashboardMetrics = {
  asOfDate: string;
  todaysSalesAmount: number;
  todaysPurchasesAmount: number;
  totalRevenue: number;
  totalProducts: number;
  inventoryValue: number;
  lowStockProducts: number;
  outstandingCustomers: number;
  outstandingSuppliers: number;
};

export async function getDashboardMetrics(businessId: string) {
  return httpClient<DashboardMetrics>("/dashboard/metrics", { businessId });
}
