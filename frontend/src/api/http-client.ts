import { appConfig } from "@/app/config";
import { readStoredAuthSession } from "@/features/auth/auth-storage";

type HttpMethod = "GET" | "POST" | "PUT" | "PATCH" | "DELETE";

export class ApiError extends Error {
  readonly status: number;
  readonly details?: unknown;

  constructor(message: string, status: number, details?: unknown) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.details = details;
  }
}

type RequestOptions = {
  method?: HttpMethod;
  body?: unknown;
  headers?: HeadersInit;
  signal?: AbortSignal;
  businessId?: string | null;
};

async function parseResponse(response: Response) {
  const contentType = response.headers.get("content-type") ?? "";

  if (contentType.includes("application/json")) {
    return response.json();
  }

  return response.text();
}

function extractErrorMessage(payload: unknown): string {
  if (typeof payload !== "object" || payload === null) {
    return "Request failed";
  }

  const fieldErrors =
    "fieldErrors" in payload &&
    typeof payload.fieldErrors === "object" &&
    payload.fieldErrors !== null
      ? Object.values(payload.fieldErrors as Record<string, unknown>).filter(
          (value): value is string => typeof value === "string" && value.length > 0,
        )
      : [];

  if (fieldErrors.length > 0) {
    return fieldErrors.join(". ");
  }

  if ("message" in payload && typeof payload.message === "string") {
    return payload.message;
  }

  return "Request failed";
}

export async function httpClient<T>(
  path: string,
  { method = "GET", body, headers, signal, businessId }: RequestOptions = {},
): Promise<T> {
  const authSession = readStoredAuthSession();

  const response = await fetch(`${appConfig.apiBaseUrl}${path}`, {
    method,
    signal,
    headers: {
      "Content-Type": "application/json",
      ...(authSession ? { Authorization: `Bearer ${authSession.accessToken}` } : {}),
      ...(businessId ? { "X-Business-Id": businessId } : {}),
      ...headers,
    },
    body: body ? JSON.stringify(body) : undefined,
  });

  const payload = await parseResponse(response);

  if (!response.ok) {
    throw new ApiError(extractErrorMessage(payload), response.status, payload);
  }

  return payload as T;
}
