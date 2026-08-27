export type NumericDraft = number | "";

export function parseNumericDraft(value: string): NumericDraft {
  if (value.trim() === "") {
    return "";
  }

  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : "";
}

export function resolveNumericDraft(value: NumericDraft, fallback = 0): number {
  return value === "" ? fallback : value;
}
