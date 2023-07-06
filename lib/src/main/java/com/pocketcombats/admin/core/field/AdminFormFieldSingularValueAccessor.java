package com.pocketcombats.admin.core.field;

import jakarta.annotation.Nullable;
import org.springframework.validation.BindingResult;

public interface AdminFormFieldSingularValueAccessor extends AdminFormFieldValueAccessor {

    void setValue(Object instance, @Nullable String value, BindingResult bindingResult);
}
