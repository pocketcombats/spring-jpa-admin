package com.pocketcombats.admin.test;

/**
 * Validation message keys as the admin UI publishes them.
 * Deliberately literal and never referencing production constants.
 */
public final class TestMessages {

    public static final String INVALID_VALUE_CODE = "spring-jpa-admin.validation.constraints.ValidValue.message";
    public static final String INVALID_ID_CODE = "spring-jpa-admin.validation.constraints.ValidId.message";
    public static final String UNIQUENESS_VIOLATION_FIELDS_CODE = "spring-jpa-admin.validation.uniqueness-violation.fields.message";

    private TestMessages() {
    }
}
