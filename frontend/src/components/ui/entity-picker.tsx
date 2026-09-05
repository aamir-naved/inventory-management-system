import { useDeferredValue, useEffect, useRef, useState } from "react";
import { useQuery } from "@tanstack/react-query";

import type { PagedResult } from "@/api/paging";

type EntityPickerProps<T> = {
  id: string;
  value: string;
  onChange: (id: string, item?: T) => void;
  enabled: boolean;
  placeholder?: string;
  queryKey: readonly unknown[];
  fetchPage: (search: string) => Promise<PagedResult<T>>;
  fetchById?: (id: string) => Promise<T>;
  getId: (item: T) => string;
  getLabel: (item: T) => string;
};

export function EntityPicker<T>({
  id,
  value,
  onChange,
  enabled,
  placeholder = "Search and select",
  queryKey,
  fetchPage,
  fetchById,
  getId,
  getLabel,
}: EntityPickerProps<T>) {
  const [open, setOpen] = useState(false);
  const [search, setSearch] = useState("");
  const deferredSearch = useDeferredValue(search);
  const rootRef = useRef<HTMLDivElement>(null);

  const pageQuery = useQuery({
    queryKey: [...queryKey, "picker", deferredSearch],
    queryFn: () => fetchPage(deferredSearch),
    enabled,
  });

  const selectedQuery = useQuery({
    queryKey: [...queryKey, "selected", value],
    queryFn: () => fetchById!(value),
    enabled: Boolean(enabled && value && fetchById),
  });

  const items = pageQuery.data?.items ?? [];
  const selected =
    items.find((item) => getId(item) === value) ?? selectedQuery.data ?? undefined;
  const closedLabel = selected ? getLabel(selected) : "";

  useEffect(() => {
    function handlePointerDown(event: MouseEvent) {
      if (!rootRef.current?.contains(event.target as Node)) {
        setOpen(false);
        setSearch("");
      }
    }

    document.addEventListener("mousedown", handlePointerDown);
    return () => document.removeEventListener("mousedown", handlePointerDown);
  }, []);

  return (
    <div className="entity-picker" ref={rootRef}>
      <input
        id={id}
        value={open ? search : closedLabel}
        placeholder={placeholder}
        autoComplete="off"
        onFocus={() => setOpen(true)}
        onChange={(event) => {
          setSearch(event.target.value);
          setOpen(true);
        }}
      />
      {open ? (
        <ul className="entity-picker__menu" role="listbox">
          {pageQuery.isLoading ? (
            <li className="entity-picker__empty">Searching...</li>
          ) : items.length === 0 ? (
            <li className="entity-picker__empty">No matches</li>
          ) : (
            items.map((item) => (
              <li key={getId(item)}>
                <button
                  type="button"
                  className={
                    getId(item) === value ? "entity-picker__option entity-picker__option--selected" : "entity-picker__option"
                  }
                  onMouseDown={(event) => event.preventDefault()}
                  onClick={() => {
                    onChange(getId(item), item);
                    setOpen(false);
                    setSearch("");
                  }}
                >
                  {getLabel(item)}
                </button>
              </li>
            ))
          )}
        </ul>
      ) : null}
    </div>
  );
}
