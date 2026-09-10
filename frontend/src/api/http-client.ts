import { appConfig } from "@/app/config";
import { expireAuthSession, refreshSessionOnce } from "@/api/auth-session";
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
  skipAuthRefresh?: boolean;
};

function isFormData(body: unknown): body is FormData {
  return typeof FormData !== "undefined" && body instanceof FormData;
}

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
  {
    method = "GET",
    body,
    headers,
    signal,
    businessId,
    skipAuthRefresh = false,
  }: RequestOptions = {},
): Promise<T> {
  const authSession = readStoredAuthSession();
  const formData = isFormData(body);
  const requestHeaders: Record<string, string> = {
    ...(formData ? {} : { "Content-Type": "application/json" }),
    ...(authSession ? { Authorization: `Bearer ${authSession.accessToken}` } : {}),
  };
  if (businessId && !path.startsWith("/platform")) {
    requestHeaders["X-Business-Id"] = businessId;
  }

  const response = await fetch(`${appConfig.apiBaseUrl}${path}`, {
    method,
    signal,
    headers: {
      ...requestHeaders,
      ...headers,
    },
    body: formData ? body : body ? JSON.stringify(body) : undefined,
  });

  if (response.status === 401 && !skipAuthRefresh && path !== "/auth/refresh") {
    const refreshed = await refreshSessionOnce();
    if (refreshed) {
      return httpClient<T>(path, {
        method,
        body,
        headers,
        signal,
        businessId,
        skipAuthRefresh: true,
      });
    }
    expireAuthSession();
  }

  if (response.status === 204) {
    return undefined as T;
  }

  const payload = await parseResponse(response);

  if (!response.ok) {
    throw new ApiError(extractErrorMessage(payload), response.status, payload);
  }

  return payload as T;
}
