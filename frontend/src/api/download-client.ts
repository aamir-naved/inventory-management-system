import { appConfig } from "@/app/config";
import { expireAuthSession, refreshSessionOnce } from "@/api/auth-session";
import { ApiError } from "@/api/http-client";
import { readStoredAuthSession } from "@/features/auth/auth-storage";

type DownloadOptions = {
  businessId?: string | null;
  signal?: AbortSignal;
  skipAuthRefresh?: boolean;
  accept?: string;
};

function filenameFromDisposition(header: string | null, fallback: string) {
  if (!header) {
    return fallback;
  }

  const utfMatch = /filename\*=UTF-8''([^;]+)/i.exec(header);
  if (utfMatch?.[1]) {
    return decodeURIComponent(utfMatch[1]);
  }

  const plainMatch = /filename="?([^"]+)"?/i.exec(header);
  if (plainMatch?.[1]) {
    return plainMatch[1];
  }

  return fallback;
}

export async function downloadBlob(
  path: string,
  {
    businessId,
    signal,
    skipAuthRefresh = false,
    accept = "application/pdf",
  }: DownloadOptions = {},
  fallbackFilename = "download.bin",
): Promise<{ blob: Blob; filename: string }> {
  const authSession = readStoredAuthSession();

  const response = await fetch(`${appConfig.apiBaseUrl}${path}`, {
    method: "GET",
    signal,
    headers: {
      ...(authSession ? { Authorization: `Bearer ${authSession.accessToken}` } : {}),
      ...(businessId ? { "X-Business-Id": businessId } : {}),
      Accept: accept,
    },
  });

  if (response.status === 401 && !skipAuthRefresh) {
    const refreshed = await refreshSessionOnce();
    if (refreshed) {
      return downloadBlob(
        path,
        { businessId, signal, skipAuthRefresh: true, accept },
        fallbackFilename,
      );
    }
    expireAuthSession();
  }

  if (!response.ok) {
    let details: unknown = undefined;
    try {
      details = await response.json();
    } catch {
      details = undefined;
    }

    const message =
      typeof details === "object" &&
      details !== null &&
      "message" in details &&
      typeof details.message === "string"
        ? details.message
        : "Download failed";

    throw new ApiError(message, response.status, details);
  }

  const blob = await response.blob();
  const filename = filenameFromDisposition(
    response.headers.get("Content-Disposition"),
    fallbackFilename,
  );

  return { blob, filename };
}

export async function openPdfInNewTab(
  path: string,
  businessId: string,
  fallbackFilename: string,
) {
  const { blob } = await downloadBlob(path, { businessId }, fallbackFilename);
  const objectUrl = URL.createObjectURL(blob);
  const opened = window.open(objectUrl, "_blank", "noopener,noreferrer");
  if (!opened) {
    URL.revokeObjectURL(objectUrl);
    throw new Error("Pop-up blocked. Allow pop-ups to print the PDF.");
  }
  window.setTimeout(() => URL.revokeObjectURL(objectUrl), 60_000);
}

export async function savePdfDownload(
  path: string,
  businessId: string,
  fallbackFilename: string,
) {
  return saveFileDownload(path, businessId, fallbackFilename, "application/pdf");
}

export async function saveFileDownload(
  path: string,
  businessId: string,
  fallbackFilename: string,
  accept: string,
) {
  const { blob, filename } = await downloadBlob(
    path,
    { businessId, accept },
    fallbackFilename,
  );
  const objectUrl = URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = objectUrl;
  anchor.download = filename;
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  window.setTimeout(() => URL.revokeObjectURL(objectUrl), 60_000);
}
