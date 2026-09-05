package com.inventory.auth.support;

import java.util.regex.Pattern;

public final class PhoneNumbers {

    private static final Pattern DIGITS = Pattern.compile("\\D+");

    private PhoneNumbers() {
    }

    public static String normalize(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("Mobile number is required");
        }
        String digits = DIGITS.matcher(raw.trim()).replaceAll("");
        if (digits.startsWith("0") && digits.length() == 11) {
            digits = digits.substring(1);
        }
        if (digits.length() == 10) {
            return "+91" + digits;
        }
        if (digits.length() == 12 && digits.startsWith("91")) {
            return "+" + digits;
        }
        if (digits.length() == 11 && digits.startsWith("91")) {
            throw new IllegalArgumentException("Enter a 10-digit Indian mobile number");
        }
        throw new IllegalArgumentException("Enter a 10-digit Indian mobile number");
    }
}
