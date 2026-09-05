import { useEffect, useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";

import { ApiError } from "@/api/http-client";
import {
  createBusiness,
  getBusiness,
  removeBusinessLogo,
  updateBusiness,
  uploadBusinessLogo,
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
  gstEnabled: false,
  gstin: "",
  stateCode: "",
  stateName: "",
  gstInclusivePricing: false,
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
        gstEnabled: businessQuery.data.gstEnabled,
        gstin: businessQuery.data.gstin ?? "",
        stateCode: businessQuery.data.stateCode ?? "",
        stateName: businessQuery.data.stateName ?? "",
        gstInclusivePricing: businessQuery.data.gstInclusivePricing,
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
          Create your shop profile once. Products, stock, purchases, and sales
          all use these details.
        </p>
      </section>

      <section className="panel">
        <h3>{session?.businessId ? "Update business details" : "Create your business profile"}</h3>
        <p>
          Enter the name, type, address, mobile, currency, and time zone for
          your business.
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

          <label className="toggle">
            <input
              id="gstEnabled"
              type="checkbox"
              checked={Boolean(form.gstEnabled)}
              onChange={(event) => updateField("gstEnabled", event.target.checked)}
            />
            <span>Enable GST on invoices and bills</span>
          </label>

          {form.gstEnabled ? (
            <>
              <div className="field">
                <label htmlFor="gstin">GSTIN</label>
                <input
                  id="gstin"
                  value={form.gstin ?? ""}
                  maxLength={15}
                  onChange={(event) => updateField("gstin", event.target.value.toUpperCase())}
                  placeholder="22AAAAA0000A1Z5"
                />
              </div>
              <div className="split-grid">
                <div className="field">
                  <label htmlFor="stateCode">State code</label>
                  <input
                    id="stateCode"
                    value={form.stateCode ?? ""}
                    maxLength={2}
                    onChange={(event) => updateField("stateCode", event.target.value.toUpperCase())}
                    placeholder="29"
                  />
                </div>
                <div className="field">
                  <label htmlFor="stateName">State</label>
                  <input
                    id="stateName"
                    value={form.stateName ?? ""}
                    onChange={(event) => updateField("stateName", event.target.value)}
                    placeholder="Karnataka"
                  />
                </div>
              </div>
              <label className="toggle">
                <input
                  type="checkbox"
                  checked={Boolean(form.gstInclusivePricing)}
                  onChange={(event) => updateField("gstInclusivePricing", event.target.checked)}
                />
                <span>Prices include GST</span>
              </label>
            </>
          ) : null}

          {session?.businessId ? (
            <div className="field">
              <label htmlFor="logo">Shop logo (PNG or JPEG, 512 KB)</label>
              <input
                id="logo"
                type="file"
                accept="image/png,image/jpeg"
                onChange={(event) => {
                  const file = event.target.files?.[0];
                  if (file && session.businessId) {
                    void uploadBusinessLogo(session.businessId, file).then((business) => {
                      updateBusinessSession({ id: business.id, name: business.name });
                      setFeedback("Logo uploaded. It will appear on invoices.");
                    });
                  }
                }}
              />
              {businessQuery.data?.hasLogo ? (
                <button
                  type="button"
                  className="ghost-button"
                  onClick={() => {
                    if (session.businessId) {
                      void removeBusinessLogo(session.businessId).then(() => {
                        setFeedback("Logo removed.");
                        void businessQuery.refetch();
                      });
                    }
                  }}
                >
                  Remove logo
                </button>
              ) : null}
            </div>
          ) : null}

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
