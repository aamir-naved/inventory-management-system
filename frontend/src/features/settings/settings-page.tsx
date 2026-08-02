import { useEffect, useState, type FormEvent } from "react";
import { Link } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { ApiError } from "@/api/http-client";
import { useAuth } from "@/features/auth/auth-context";
import { formatDate, formatMoney } from "@/features/settings/format";
import {
  DATE_FORMAT_OPTIONS,
  getSettings,
  updateSettings,
  type SettingsPayload,
} from "@/features/settings/settings-api";

const initialForm: SettingsPayload = {
  currencyCode: "INR",
  dateFormat: "dd/MM/yyyy",
  allowNegativeStock: false,
  defaultLowStockThreshold: 0,
};

export function SettingsPage() {
  const queryClient = useQueryClient();
  const { session } = useAuth();
  const businessId = session?.businessId ?? null;
  const [form, setForm] = useState<SettingsPayload>(initialForm);
  const [feedback, setFeedback] = useState<string | null>(null);
  const [feedbackTone, setFeedbackTone] = useState<"error" | "success">("error");
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  const settingsQuery = useQuery({
    queryKey: ["settings", businessId],
    queryFn: () => getSettings(businessId!),
    enabled: Boolean(businessId),
  });

  useEffect(() => {
    if (settingsQuery.data) {
      setForm({
        currencyCode: settingsQuery.data.currencyCode,
        dateFormat: settingsQuery.data.dateFormat,
        allowNegativeStock: settingsQuery.data.allowNegativeStock,
        defaultLowStockThreshold: Number(
          settingsQuery.data.defaultLowStockThreshold,
        ),
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

  function updateField<K extends keyof SettingsPayload>(
    key: K,
    value: SettingsPayload[K],
  ) {
    setForm((current) => ({ ...current, [key]: value }));
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setFeedback(null);
    setFieldErrors({});
    await saveMutation.mutateAsync({
      ...form,
      currencyCode: form.currencyCode.trim().toUpperCase(),
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
              <label htmlFor="currencyCode">Currency</label>
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
              <label htmlFor="dateFormat">Date format</label>
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
              <label htmlFor="defaultLowStockThreshold">
                Default low-stock threshold
              </label>
              <input
                id="defaultLowStockThreshold"
                type="number"
                min="0"
                step="0.001"
                value={form.defaultLowStockThreshold}
                onChange={(event) =>
                  updateField(
                    "defaultLowStockThreshold",
                    Number(event.target.value),
                  )
                }
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
    </>
  );
}
