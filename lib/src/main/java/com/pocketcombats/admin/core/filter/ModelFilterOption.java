package com.pocketcombats.admin.core.filter;

public record ModelFilterOption(
        String label,
        String value,
        boolean localize
) {
    public ModelFilterOption(String label, String value) {
        this(label, value, false);
    }
}
