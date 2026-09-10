package com.inventory.document.numbering;

public enum DocumentType {
    SALE("SAL"),
    PURCHASE("PUR"),
    SALE_RETURN("RET"),
    PURCHASE_RETURN("PRT");

    private final String prefix;

    DocumentType(String prefix) {
        this.prefix = prefix;
    }

    public String prefix() {
        return prefix;
    }
}
