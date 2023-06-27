package com.pocketcombats.admin;

@FunctionalInterface
public interface ValueFormatter {

    String format(Object entity);
}
