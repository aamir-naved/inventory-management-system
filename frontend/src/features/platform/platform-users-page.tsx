import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { ApiError } from "@/api/http-client";
import { PaginationBar } from "@/components/ui/pagination-bar";
import { listPlatformUsers, updatePlatformUser } from "@/features/platform/platform-api";

export function PlatformUsersPage() {
  const queryClient = useQueryClient();
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(0);
  const [feedback, setFeedback] = useState<string | null>(null);

  const usersQuery = useQuery({
    queryKey: ["platform-users", search, page],
    queryFn: () => listPlatformUsers({ search, page }),
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, active }: { id: string; active: boolean }) => updatePlatformUser(id, { active }),
    onSuccess: () => {
      setFeedback("User updated.");
      void queryClient.invalidateQueries({ queryKey: ["platform-users"] });
    },
    onError: (error) => {
      setFeedback(error instanceof ApiError ? error.message : "Unable to update the user.");
    },
  });

  return (
    <>
      <section className="page-intro">
        <span className="brand-kicker">Directory</span>
        <h1>Users</h1>
        <p>Disable a shop user if you need to cut access without deleting history.</p>
      </section>
      <section className="panel">
        <div className="field">
          <label htmlFor="user-search">Search</label>
          <input
            id="user-search"
            value={search}
            onChange={(event) => {
              setSearch(event.target.value);
              setPage(0);
            }}
            placeholder="Name, email, or phone"
          />
        </div>
        {feedback ? <p className="inline-note">{feedback}</p> : null}
        <ul className="list">
          {(usersQuery.data?.items ?? []).map((user) => (
            <li key={user.id} className="product-card">
              <strong>{user.fullName}</strong>
              <p>
                {user.platformRole === "PLATFORM_ADMIN" ? "Platform admin" : user.membershipRole ?? "No shop"}
                {user.shopName ? ` · ${user.shopName}` : ""} · {user.active ? "Active" : "Disabled"}
              </p>
              <p>{user.email ?? "no email"} · {user.phone ?? "no phone"}</p>
              <button
                type="button"
                className="ghost-button"
                disabled={updateMutation.isPending}
                onClick={() => updateMutation.mutate({ id: user.id, active: !user.active })}
              >
                {user.active ? "Disable" : "Enable"}
              </button>
            </li>
          ))}
        </ul>
        <PaginationBar page={usersQuery.data} onPageChange={setPage} />
      </section>
    </>
  );
}
