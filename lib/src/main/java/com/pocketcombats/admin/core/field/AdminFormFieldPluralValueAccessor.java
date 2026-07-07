package com.pocketcombats.admin.core.field;

import org.jspecify.annotations.Nullable;
import org.springframework.validation.BindingResult;

import java.util.List;

public interface AdminFormFieldPluralValueAccessor extends AdminFormFieldValueAccessor {

    void setValues(Object instance, @Nullable List<String> values, BindingResult bindingResult);
}
