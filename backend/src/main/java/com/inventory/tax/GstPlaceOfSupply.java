package com.inventory.tax;

/**
 * Derives CGST/SGST vs IGST from place of supply (party state vs business state).
 * Missing party state defaults to intrastate (typical B2C / walk-in counter sales).
 */
public final class GstPlaceOfSupply {

    private GstPlaceOfSupply() {
    }

    public static boolean isInterstate(String businessStateCode, String partyStateCode) {
        String business = normalize(businessStateCode);
        String party = normalize(partyStateCode);
        if (business == null || party == null) {
            return false;
        }
        return !business.equals(party);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim().toUpperCase();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
