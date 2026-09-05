import type { PagedResult } from "@/api/paging";

type PaginationBarProps = {
  page: PagedResult<unknown> | undefined;
  onPageChange: (page: number) => void;
};

function rangeLabel(page: PagedResult<unknown>) {
  if (page.totalItems === 0) {
    return "0 of 0";
  }

  const start = page.page * page.size + 1;
  const end = Math.min(page.totalItems, start + page.items.length - 1);
  return `${start}–${end} of ${page.totalItems}`;
}

export function PaginationBar({ page, onPageChange }: PaginationBarProps) {
  if (!page || page.totalPages <= 1) {
    return page && page.totalItems > 0 ? (
      <p className="pagination-bar pagination-bar--static">{rangeLabel(page)}</p>
    ) : null;
  }

  return (
    <div className="pagination-bar">
      <button
        type="button"
        className="ghost-button"
        disabled={page.page <= 0}
        onClick={() => onPageChange(page.page - 1)}
      >
        Previous
      </button>
      <span>{rangeLabel(page)}</span>
      <button
        type="button"
        className="ghost-button"
        disabled={page.page + 1 >= page.totalPages}
        onClick={() => onPageChange(page.page + 1)}
      >
        Next
      </button>
    </div>
  );
}
