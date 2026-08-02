import { useEffect, useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";

import { ApiError } from "@/api/http-client";
import {
  createBusiness,
  getBusiness,
  updateBusiness,
  type BusinessPayload,
} from "@/features/business/business-api";
import { useAuth } from "@/features/auth/auth-context";

const initialForm: BusinessPayload = {
  name: "",
  businessType: "",
  addressLine: "",
  mobileNumber: "",
  currencyCode: "INR",
  timeZone: "Asia/Kolkata",
};

export function BusinessSetupPage() {
  const { session, updateBusinessSession } = useAuth();
  const [form, setForm] = useState<BusinessPayload>(initialForm);
  const [feedback, setFeedback] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  const businessQuery = useQuery({
    queryKey: ["business", session?.businessId],
    queryFn: () => getBusiness(session!.businessId!),
    enabled: Boolean(session?.businessId),
  });

  useEffect(() => {
    if (businessQuery.data) {
      setForm({
        name: businessQuery.data.name,
        businessType: businessQuery.data.businessType,
        addressLine: businessQuery.data.addressLine ?? "",
        mobileNumber: businessQuery.data.mobileNumber,
        currencyCode: businessQuery.data.currencyCode,
        timeZone: businessQuery.data.timeZone,
      });
    }
  }, [businessQuery.data]);

  const saveMutation = useMutation({
    mutationFn: async (payload: BusinessPayload) => {
      if (session?.businessId) {
        return updateBusiness(session.businessId, payload);
      }

      return createBusiness(payload);
    },
    onSuccess: (business) => {
      updateBusinessSession({ id: business.id, name: business.name });
      setFeedback("Business details saved successfully.");
      setFieldErrors({});
    },
    onError: (error) => {
      if (error instanceof ApiError && typeof error.details === "object" && error.details !== null) {
        const response = error.details as {
          fieldErrors?: Record<string, string>;
          message?: string;
        };

        setFieldErrors(response.fieldErrors ?? {});
        setFeedback(response.message ?? "Unable to save business details.");
        return;
      }

      setFeedback("Unable to save business details.");
    },
  });

  function updateField<K extends keyof BusinessPayload>(key: K, value: BusinessPayload[K]) {
    setForm((current) => ({ ...current, [key]: value }));
  }

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setFeedback(null);
    setFieldErrors({});
    await saveMutation.mutateAsync(form);
  }

  return (
    <>
      <section className="page-intro">
        <span className="brand-kicker">Business setup</span>
        <h1>Set up the business that this workspace belongs to.</h1>
        <p>
          This is the first real product flow: create the business profile, lock
          in the tenant identity, and reuse it across future modules.
        </p>
      </section>

      <section className="panel">
        <h3>{session?.businessId ? "Update business details" : "Create your business profile"}</h3>
        <p>
          These fields match the MVP setup document: name, type, address, mobile,
          currency, and time zone.
        </p>

        <form className="form-stack" onSubmit={handleSubmit}>
          <div className="field">
            <label htmlFor="name">Business name</label>
            <input
              id="name"
              value={form.name}
              onChange={(event) => updateField("name", event.target.value)}
              placeholder="North Star Traders"
            />
            {fieldErrors.name ? <span className="field-error">{fieldErrors.name}</span> : null}
          </div>

          <div className="field">
            <label htmlFor="businessType">Business type</label>
            <input
              id="businessType"
              value={form.businessType}
              onChange={(event) => updateField("businessType", event.target.value)}
              placeholder="Hardware Store"
            />
            {fieldErrors.businessType ? (
              <span className="field-error">{fieldErrors.businessType}</span>
            ) : null}
          </div>

          <div className="field">
            <label htmlFor="addressLine">Address</label>
            <input
              id="addressLine"
              value={form.addressLine}
              onChange={(event) => updateField("addressLine", event.target.value)}
              placeholder="42 Market Road"
            />
            {fieldErrors.addressLine ? (
              <span className="field-error">{fieldErrors.addressLine}</span>
            ) : null}
          </div>

          <div className="field">
            <label htmlFor="mobileNumber">Mobile number</label>
            <input
              id="mobileNumber"
              value={form.mobileNumber}
              onChange={(event) => updateField("mobileNumber", event.target.value)}
              placeholder="+91 9876543210"
            />
            {fieldErrors.mobileNumber ? (
              <span className="field-error">{fieldErrors.mobileNumber}</span>
            ) : null}
          </div>

          <div className="split-grid">
            <div className="field">
              <label htmlFor="currencyCode">Currency</label>
              <input
                id="currencyCode"
                value={form.currencyCode}
                onChange={(event) => updateField("currencyCode", event.target.value.toUpperCase())}
                placeholder="INR"
              />
              {fieldErrors.currencyCode ? (
                <span className="field-error">{fieldErrors.currencyCode}</span>
              ) : null}
            </div>

            <div className="field">
              <label htmlFor="timeZone">Time zone</label>
              <input
                id="timeZone"
                value={form.timeZone}
                onChange={(event) => updateField("timeZone", event.target.value)}
                placeholder="Asia/Kolkata"
              />
              {fieldErrors.timeZone ? (
                <span className="field-error">{fieldErrors.timeZone}</span>
              ) : null}
            </div>
          </div>

          {feedback ? <p className="inline-note">{feedback}</p> : null}
          {businessQuery.isLoading ? <p className="inline-note">Loading business details...</p> : null}

          <button type="submit" className="primary-button" disabled={saveMutation.isPending}>
            {saveMutation.isPending
              ? "Saving..."
              : session?.businessId
                ? "Save changes"
                : "Create business"}
          </button>
        </form>
      </section>
    </>
  );
}
