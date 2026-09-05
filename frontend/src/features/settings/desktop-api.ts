import { httpClient } from "@/api/http-client";
import { saveFileDownload } from "@/api/download-client";

export type DesktopInfo = {
  desktop: boolean;
  dataDir: string;
  pendingRestore: boolean;
};

export type DesktopRestoreResult = {
  restartRequired: boolean;
  message: string;
};

export function getDesktopInfo() {
  return httpClient<DesktopInfo>("/desktop/info");
}

export function downloadDesktopBackup(businessId: string) {
  return saveFileDownload(
    "/desktop/backup",
    businessId,
    "inventory-backup.sql.gz",
    "application/gzip",
  );
}

export function uploadDesktopRestore(file: File) {
  const body = new FormData();
  body.append("file", file);
  return httpClient<DesktopRestoreResult>("/desktop/restore", {
    method: "POST",
    body,
  });
}
