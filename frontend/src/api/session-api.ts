import { httpClient } from "@/api/http-client";

export type CurrentBusiness = {
  id: string;
  name: string;
  businessType: string;
};

export type CurrentUser = {
  id: string;
  fullName: string;
  email: string;
  activeBusiness: CurrentBusiness;
};

export async function fetchCurrentUser(businessId: string) {
  return httpClient<CurrentUser>("/me", { businessId });
}
