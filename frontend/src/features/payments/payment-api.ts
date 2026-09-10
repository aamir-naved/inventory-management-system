import { httpClient } from "@/api/http-client";

export type PaymentPayload = {
  paymentDate: string;
  amount: number;
  notes: string;
};

export type PaymentRecord = {
  id: string;
  businessId: string;
  partyType: "CUSTOMER" | "SUPPLIER";
  partyId: string;
  documentType: "SALE" | "PURCHASE";
  documentId: string;
  paymentDate: string;
  amount: number;
  paymentKind: "RECEIPT" | "REFUND";
  notes: string | null;
  createdAt: string;
  updatedAt: string;
};

export async function listSalePayments(businessId: string, saleId: string) {
  return httpClient<PaymentRecord[]>(`/sales/${saleId}/payments`, {
    businessId,
  });
}

export async function createSalePayment(
  businessId: string,
  saleId: string,
  payload: PaymentPayload,
  idempotencyKey?: string,
) {
  return httpClient<PaymentRecord>(`/sales/${saleId}/payments`, {
    method: "POST",
    businessId,
    body: payload,
    headers: idempotencyKey ? { "Idempotency-Key": idempotencyKey } : undefined,
  });
}

export async function listPurchasePayments(businessId: string, purchaseId: string) {
  return httpClient<PaymentRecord[]>(`/purchases/${purchaseId}/payments`, {
    businessId,
  });
}

export async function createPurchasePayment(
  businessId: string,
  purchaseId: string,
  payload: PaymentPayload,
) {
  return httpClient<PaymentRecord>(`/purchases/${purchaseId}/payments`, {
    method: "POST",
    businessId,
    body: payload,
  });
}
