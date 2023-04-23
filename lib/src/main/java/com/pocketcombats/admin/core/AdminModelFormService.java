package com.pocketcombats.admin.core;

import com.pocketcombats.admin.data.form.EntityDetails;

import java.util.Map;

public interface AdminModelFormService {

    EntityDetails details(String modelName, String id) throws UnknownModelException;

    AdminModelEditingResult update(String modelName, String id, Map<String, String> data) throws UnknownModelException;
}
