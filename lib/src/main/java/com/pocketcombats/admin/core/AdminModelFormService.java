package com.pocketcombats.admin.core;

import com.pocketcombats.admin.data.form.EntityDetails;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.BindingResult;

public interface AdminModelFormService {

    EntityDetails details(String modelName, String id) throws UnknownModelException;

    AdminModelEditingResult update(
            String modelName,
            String id,
            MultiValueMap<String, String> data
    ) throws UnknownModelException;

    BindingResult updateField(
            String modelName,
            String stringId,
            String fieldName,
            String value
    ) throws UnknownModelException;

    EntityDetails create(String modelName) throws UnknownModelException;

    AdminModelEditingResult create(String modelName, MultiValueMap<String, String> data) throws UnknownModelException;
}
