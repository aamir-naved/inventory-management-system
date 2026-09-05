export const DEFAULT_PAGE_SIZE = 25;

export type PagedResult<T> = {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
};

export type PageRequest = {
  page?: number;
  size?: number;
};

export function withPaging(params: URLSearchParams, paging: PageRequest = {}) {
  params.set("page", String(paging.page ?? 0));
  params.set("size", String(paging.size ?? DEFAULT_PAGE_SIZE));
  return params;
}
