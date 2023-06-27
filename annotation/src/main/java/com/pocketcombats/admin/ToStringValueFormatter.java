package com.pocketcombats.admin;

public class ToStringValueFormatter implements ValueFormatter {

    @Override
    public String format(Object entity) {
        return entity.toString();
    }
}
