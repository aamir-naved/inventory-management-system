import { useQuery } from "@tanstack/react-query";

import { getPlatformSettings } from "@/features/platform/platform-api";

export function PlatformSettingsPage() {
  const settingsQuery = useQuery({
    queryKey: ["platform-settings"],
    queryFn: getPlatformSettings,
  });

  return (
    <>
      <section className="page-intro">
        <span className="brand-kicker">Ops</span>
        <h1>Settings</h1>
        <p>These flags are set in the server environment, not from this screen.</p>
      </section>
      <section className="panel">
        {settingsQuery.isError ? <p className="form-error">Unable to load settings.</p> : null}
        <p>
          Open registration:{" "}
          <strong>{settingsQuery.data?.openRegistration ? "on (self-serve shops)" : "off (admin creates shops)"}</strong>
        </p>
        <p className="inline-note">
          Change <code>APP_OPEN_REGISTRATION</code> and restart the API. Production default is off.
        </p>
      </section>
    </>
  );
}
