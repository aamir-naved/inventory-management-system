import { useState, type ReactNode } from "react";

export function FieldInfo({ label, children }: { label: string; children: ReactNode }) {
  const [open, setOpen] = useState(false);

  return (
    <span className={`field-info${open ? " field-info--open" : ""}`}>
      <button
        type="button"
        className="field-info__button"
        aria-label={`About ${label}`}
        aria-expanded={open}
        onClick={() => setOpen((current) => !current)}
        onBlur={() => setOpen(false)}
      >
        i
      </button>
      <span className="field-info__tooltip" role="tooltip">
        {children}
      </span>
    </span>
  );
}

export function FieldLabel({
  htmlFor,
  label,
  info,
}: {
  htmlFor: string;
  label: string;
  info?: ReactNode;
}) {
  return (
    <div className="field-label-row">
      <label htmlFor={htmlFor}>{label}</label>
      {info ? <FieldInfo label={label}>{info}</FieldInfo> : null}
    </div>
  );
}
