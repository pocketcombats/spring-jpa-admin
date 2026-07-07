package com.pocketcombats.admin.core.field;

import org.springframework.util.MultiValueMap;
import org.springframework.validation.BindingResult;

/**
 * Form field accessor that binds multiple request parameters, submitted under
 * {@code <field parameter>.<property>} names, onto a single field value.
 */
public interface AdminFormFieldCompositeValueAccessor {

    void bind(
            String parameterName,
            Object instance,
            MultiValueMap<String, String> rawData,
            BindingResult bindingResult
    );
}
