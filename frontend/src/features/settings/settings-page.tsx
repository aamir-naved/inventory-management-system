import { useEffect, useState, type FormEvent } from "react";
import { Link } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { ApiError } from "@/api/http-client";
import { FieldLabel } from "@/components/ui/field-label";
import { useAuth } from "@/features/auth/auth-context";
import { getPublicConfig } from "@/features/auth/auth-api";
import { formatDate, formatMoney } from "@/features/settings/format";
import {
  DATE_FORMAT_OPTIONS,
  getSettings,
  updateSettings,
  type SettingsPayload,
} from "@/features/settings/settings-api";
import {
  downloadDesktopBackup,
  getDesktopInfo,
  uploadDesktopRestore,
} from "@/features/settings/desktop-api";
import {
  parseNumericDraft,
  resolveNumericDraft,
  type NumericDraft,
} from "@/lib/numeric-draft";

type SettingsFormState = {
  currencyCode: string;
  dateFormat: string;
  allowNegativeStock: boolean;
  defaultLowStockThreshold: NumericDraft;
  gstEnabled: boolean;
  gstin: string;
  stateCode: string;
  stateName: string;
  gstInclusivePricing: boolean;
};

const initialForm: SettingsFormState = {
  currencyCode: "INR",
  dateFormat: "dd/MM/yyyy",
  allowNegativeStock: false,
  defaultLowStockThreshold: "",
  gstEnabled: false,
  gstin: "",
  stateCode: "",
  stateName: "",
  gstInclusivePricing: false,
};

export function SettingsPage() {
  const queryClient = useQueryClient();
  const { session } = useAuth();
  const businessId = session?.businessId ?? null;
  const [form, setForm] = useState<SettingsFormState>(initialForm);
  const [feedback, setFeedback] = useState<string | null>(null);
  const [feedbackTone, setFeedbackTone] = useState<"error" | "success">("error");
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [restoreFile, setRestoreFile] = useState<File | null>(null);
  const [backupMessage, setBackupMessage] = useState<string | null>(null);
  const [backupTone, setBackupTone] = useState<"error" | "success">("error");

  const settingsQuery = useQuery({
    queryKey: ["settings", businessId],
    queryFn: () => getSettings(businessId!),
    enabled: Boolean(businessId),
  });
  const publicConfigQuery = useQuery({
    queryKey: ["public-config"],
    queryFn: getPublicConfig,
  });
  const desktop = publicConfigQuery.data?.desktop === true;
  const desktopInfoQuery = useQuery({
    queryKey: ["desktop-info"],
    queryFn: getDesktopInfo,
    enabled: desktop,
  });

  useEffect(() => {
    if (settingsQuery.data) {
      setForm({
        currencyCode: settingsQuery.data.currencyCode,
        dateFormat: settingsQuery.data.dateFormat,
        allowNegativeStock: settingsQuery.data.allowNegativeStock,
        defaultLowStockThreshold: Number(settingsQuery.data.defaultLowStockThreshold),
        gstEnabled: settingsQuery.data.gstEnabled,
        gstin: settingsQuery.data.gstin ?? "",
        stateCode: settingsQuery.data.stateCode ?? "",
        stateName: settingsQuery.data.stateName ?? "",
        gstInclusivePricing: settingsQuery.data.gstInclusivePricing,
      });
    }
  }, [settingsQuery.data]);

  const saveMutation = useMutation({
    mutationFn: (payload: SettingsPayload) => updateSettings(businessId!, payload),
    onSuccess: (settings) => {
      queryClient.setQueryData(["settings", businessId], settings);
      void queryClient.invalidateQueries({ queryKey: ["settings", businessId] });
      setFeedbackTone("success");
      setFeedback("Settings saved.");
      setFieldErrors({});
    },
    onError: (error) => {
      setFeedbackTone("error");
      if (error instanceof ApiError && typeof error.details === "object" && error.details !== null) {
        const response = error.details as {
          fieldErrors?: Record<string, string>;
          message?: string;
        };
        setFieldErrors(response.fieldErrors ?? {});
        setFeedback(response.message ?? error.message);
        return;
      }
      setFeedback(error instanceof Error ? error.message : "Unable to save settings.");
    },
  });

  function updateField<K extends keyof SettingsFormState>(
    key: K,
    value: SettingsFormState[K],
  ) {
    setForm((current) => ({ ...current, [key]: value }));
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setFeedback(null);
    setFieldErrors({});
    await saveMutation.mutateAsync({
      currencyCode: form.currencyCode.trim().toUpperCase(),
      dateFormat: form.dateFormat,
      allowNegativeStock: form.allowNegativeStock,
      defaultLowStockThreshold: resolveNumericDraft(form.defaultLowStockThreshold),
      gstEnabled: form.gstEnabled,
      gstin: form.gstin,
      stateCode: form.stateCode,
      stateName: form.stateName,
      gstInclusivePricing: form.gstInclusivePricing,
    });
  }

  if (!businessId) {
    return (
      <>
        <section className="page-intro">
          <span className="brand-kicker">Settings</span>
          <h1>Business preferences for stock and display.</h1>
          <p>Finish business setup before configuring workspace settings.</p>
        </section>
        <section className="panel">
          <Link to="/business-setup" className="primary-button">
            Complete business setup
          </Link>
        </section>
      </>
    );
  }

  return (
    <>
      <section className="page-intro">
        <span className="brand-kicker">Settings</span>
        <h1>Business preferences for stock and display.</h1>
        <p>
          Control negative stock, default low-stock alerts, currency, and how
          dates appear across the workspace.
        </p>
      </section>

      <section className="panel">
        <h3>Workspace preferences</h3>
        {settingsQuery.isLoading ? (
          <p className="inline-note">Loading settings…</p>
        ) : (
          <form className="form-stack" onSubmit={handleSubmit}>
            <div className="field">
              <FieldLabel
                htmlFor="currencyCode"
                label="Currency"
                info="3-letter currency code used for money across the app, such as INR or USD."
              />
              <input
                id="currencyCode"
                value={form.currencyCode}
                maxLength={3}
                onChange={(event) =>
                  updateField("currencyCode", event.target.value.toUpperCase())
                }
                placeholder="INR"
              />
              {fieldErrors.currencyCode ? (
                <span className="field-error">{fieldErrors.currencyCode}</span>
              ) : (
                <span className="inline-note">
                  Preview: {formatMoney(1234.5, form.currencyCode || "INR")}
                </span>
              )}
            </div>

            <div className="field">
              <FieldLabel
                htmlFor="dateFormat"
                label="Date format"
                info="How dates appear on sales, purchases, invoices, and reports."
              />
              <select
                id="dateFormat"
                value={form.dateFormat}
                onChange={(event) => updateField("dateFormat", event.target.value)}
              >
                {DATE_FORMAT_OPTIONS.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.value} ({option.label})
                  </option>
                ))}
              </select>
              {fieldErrors.dateFormat ? (
                <span className="field-error">{fieldErrors.dateFormat}</span>
              ) : (
                <span className="inline-note">
                  Preview: {formatDate("2026-12-31", form.dateFormat)}
                </span>
              )}
            </div>

            <div className="field">
              <FieldLabel
                htmlFor="defaultLowStockThreshold"
                label="Default low-stock threshold"
                info="Starting low-stock value for new products. When stock reaches this level, the product is flagged so you can reorder."
              />
              <input
                id="defaultLowStockThreshold"
                type="number"
                min="0"
                step="0.001"
                inputMode="decimal"
                value={form.defaultLowStockThreshold}
                onChange={(event) =>
                  updateField(
                    "defaultLowStockThreshold",
                    parseNumericDraft(event.target.value),
                  )
                }
                placeholder="0"
              />
              {fieldErrors.defaultLowStockThreshold ? (
                <span className="field-error">
                  {fieldErrors.defaultLowStockThreshold}
                </span>
              ) : (
                <span className="inline-note">
                  Used as the starting value when creating new products.
                </span>
              )}
            </div>

            <label className="toggle" htmlFor="allowNegativeStock">
              <input
                id="allowNegativeStock"
                type="checkbox"
                checked={form.allowNegativeStock}
                onChange={(event) =>
                  updateField("allowNegativeStock", event.target.checked)
                }
              />
              <span>
                Allow negative stock on sales and adjustments
                {fieldErrors.allowNegativeStock ? (
                  <span className="field-error">
                    {" "}
                    {fieldErrors.allowNegativeStock}
                  </span>
                ) : null}
              </span>
            </label>

            <label className="toggle" htmlFor="gstEnabled">
              <input
                id="gstEnabled"
                type="checkbox"
                checked={form.gstEnabled}
                onChange={(event) => updateField("gstEnabled", event.target.checked)}
              />
              <span>Enable GST on invoices</span>
            </label>

            {form.gstEnabled ? (
              <>
                <div className="field">
                  <label htmlFor="settings-gstin">GSTIN</label>
                  <input
                    id="settings-gstin"
                    value={form.gstin}
                    maxLength={15}
                    onChange={(event) => updateField("gstin", event.target.value.toUpperCase())}
                  />
                </div>
                <div className="split-grid">
                  <div className="field">
                    <label htmlFor="settings-state-code">State code</label>
                    <input
                      id="settings-state-code"
                      value={form.stateCode}
                      maxLength={2}
                      onChange={(event) =>
                        updateField("stateCode", event.target.value.toUpperCase())
                      }
                    />
                  </div>
                  <div className="field">
                    <label htmlFor="settings-state-name">State</label>
                    <input
                      id="settings-state-name"
                      value={form.stateName}
                      onChange={(event) => updateField("stateName", event.target.value)}
                    />
                  </div>
                </div>
                <label className="toggle">
                  <input
                    type="checkbox"
                    checked={form.gstInclusivePricing}
                    onChange={(event) =>
                      updateField("gstInclusivePricing", event.target.checked)
                    }
                  />
                  <span>Selling and purchase prices include GST</span>
                </label>
              </>
            ) : null}

            {feedback ? (
              <p
                className={feedbackTone === "success" ? "form-success" : "form-error"}
                role={feedbackTone === "success" ? "status" : "alert"}
              >
                {feedback}
              </p>
            ) : null}

            <button
              type="submit"
              className="primary-button"
              disabled={saveMutation.isPending}
            >
              {saveMutation.isPending ? "Saving..." : "Save settings"}
            </button>
          </form>
        )}
      </section>

      {desktop ? (
        <section className="panel">
          <h3>Backup this PC</h3>
          <p className="inline-note">
            Shop data lives in{" "}
            <code>{desktopInfoQuery.data?.dataDir ?? "this computer's app data folder"}</code>.
            Export a copy you can keep on a USB drive. Restore replaces everything on this PC
            the next time you open the app.
          </p>
          <div className="form-stack">
            <button
              type="button"
              className="ghost-button"
              onClick={() => {
                setBackupMessage(null);
                void downloadDesktopBackup(businessId)
                  .then(() => {
                    setBackupTone("success");
                    setBackupMessage("Backup downloaded.");
                  })
                  .catch((error: unknown) => {
                    setBackupTone("error");
                    setBackupMessage(
                      error instanceof Error ? error.message : "Unable to export a backup.",
                    );
                  });
              }}
            >
              Export backup
            </button>
            <div className="field">
              <FieldLabel
                htmlFor="desktop-restore"
                label="Restore backup"
                info="Use a .sql.gz file exported from this app. Close and reopen after restore."
              />
              <input
                id="desktop-restore"
                type="file"
                accept=".gz,.sql.gz"
                onChange={(event) => setRestoreFile(event.target.files?.[0] ?? null)}
              />
            </div>
            <button
              type="button"
              className="primary-button"
              disabled={!restoreFile}
              onClick={() => {
                if (!restoreFile) {
                  return;
                }
                setBackupMessage(null);
                void uploadDesktopRestore(restoreFile)
                  .then((result) => {
                    setBackupTone("success");
                    setBackupMessage(result.message);
                  })
                  .catch((error: unknown) => {
                    setBackupTone("error");
                    setBackupMessage(
                      error instanceof Error ? error.message : "Unable to queue restore.",
                    );
                  });
              }}
            >
              Queue restore
            </button>
            {desktopInfoQuery.data?.pendingRestore ? (
              <p className="inline-note">
                A restore is waiting. Close this window and open the app again to apply it.
              </p>
            ) : null}
            {backupMessage ? (
              <p
                className={backupTone === "success" ? "form-success" : "form-error"}
                role={backupTone === "success" ? "status" : "alert"}
              >
                {backupMessage}
              </p>
            ) : null}
          </div>
        </section>
      ) : null}
    </>
  );
}
