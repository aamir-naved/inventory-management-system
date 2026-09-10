import { useT } from "@/i18n/locale-context";

export function RouteFallback() {
  const t = useT();
  return (
    <section className="panel" aria-busy="true" aria-live="polite">
      <p className="inline-note">{t("common.loading")}</p>
    </section>
  );
}
