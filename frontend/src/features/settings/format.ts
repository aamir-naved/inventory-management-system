const MONTH_SHORT = [
  "Jan",
  "Feb",
  "Mar",
  "Apr",
  "May",
  "Jun",
  "Jul",
  "Aug",
  "Sep",
  "Oct",
  "Nov",
  "Dec",
];

export function formatMoney(
  value: number | undefined | null,
  currencyCode = "INR",
) {
  const amount = Number(value ?? 0);
  try {
    return new Intl.NumberFormat(undefined, {
      style: "currency",
      currency: currencyCode,
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(amount);
  } catch {
    return `${currencyCode} ${amount.toFixed(2)}`;
  }
}

export function formatDate(
  value: string | Date | null | undefined,
  dateFormat = "dd/MM/yyyy",
) {
  if (!value) {
    return "";
  }

  const date = typeof value === "string" ? parseDateInput(value) : value;
  if (!date || Number.isNaN(date.getTime())) {
    return String(value);
  }

  const day = String(date.getDate()).padStart(2, "0");
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const year = String(date.getFullYear());
  const monthShort = MONTH_SHORT[date.getMonth()];

  switch (dateFormat) {
    case "MM/dd/yyyy":
      return `${month}/${day}/${year}`;
    case "yyyy-MM-dd":
      return `${year}-${month}-${day}`;
    case "dd-MMM-yyyy":
      return `${day}-${monthShort}-${year}`;
    case "dd/MM/yyyy":
    default:
      return `${day}/${month}/${year}`;
  }
}

export function formatDateTime(
  value: string | Date | null | undefined,
  dateFormat = "dd/MM/yyyy",
) {
  if (!value) {
    return "";
  }

  const date = typeof value === "string" ? new Date(value) : value;
  if (Number.isNaN(date.getTime())) {
    return String(value);
  }

  const time = date.toLocaleTimeString(undefined, {
    hour: "2-digit",
    minute: "2-digit",
  });
  return `${formatDate(date, dateFormat)} ${time}`;
}

function parseDateInput(value: string) {
  if (/^\d{4}-\d{2}-\d{2}$/.test(value)) {
    const [year, month, day] = value.split("-").map(Number);
    return new Date(year, month - 1, day);
  }
  return new Date(value);
}
