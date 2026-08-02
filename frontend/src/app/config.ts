const fallbackApiUrl = "http://localhost:8080/api";

export const appConfig = {
  apiBaseUrl: import.meta.env.VITE_API_URL ?? fallbackApiUrl,
  appName: "Inventory Management System",
};
