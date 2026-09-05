export const GST_RATES = [0, 5, 12, 18, 28] as const;

export type GstLine = {
  taxableAmount: number;
  cgstAmount: number;
  sgstAmount: number;
  igstAmount: number;
  taxAmount: number;
  lineTotal: number;
};

function roundMoney(value: number) {
  return Math.round((value + Number.EPSILON) * 100) / 100;
}

export function computeGstLine(
  quantity: number,
  unitPrice: number,
  gstRate: number,
  inclusive: boolean,
  interstate: boolean,
): GstLine {
  const gross = quantity * unitPrice;
  const rate = gstRate / 100;
  let taxable = gross;
  let tax = 0;

  if (rate > 0) {
    if (inclusive) {
      taxable = roundMoney(gross / (1 + rate));
      tax = roundMoney(gross - taxable);
    } else {
      taxable = roundMoney(gross);
      tax = roundMoney(taxable * rate);
    }
  } else {
    taxable = roundMoney(gross);
  }

  let cgstAmount = 0;
  let sgstAmount = 0;
  let igstAmount = 0;
  if (tax > 0) {
    if (interstate) {
      igstAmount = tax;
    } else {
      cgstAmount = roundMoney(tax / 2);
      sgstAmount = roundMoney(tax - cgstAmount);
    }
  }

  return {
    taxableAmount: taxable,
    cgstAmount,
    sgstAmount,
    igstAmount,
    taxAmount: tax,
    lineTotal: roundMoney(taxable + cgstAmount + sgstAmount + igstAmount),
  };
}
