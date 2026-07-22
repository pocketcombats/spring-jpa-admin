package com.pocketcombats.admin.core;

import com.pocketcombats.admin.data.form.AdminSelectOptionsResponse;
import org.jspecify.annotations.Nullable;

/**
 * Serves paginated, searchable options for autocomplete widgets.
 */
public interface AdminModelOptionsService {

    /**
     * A page of options for the given model's field, filtered by an optional search query.
     */
    AdminSelectOptionsResponse options(
            String modelName,
            String fieldName,
            @Nullable String query,
            int page
    ) throws UnknownModelException;

    /**
     * Resolves the single option matching the given submit-ready value, used to prefill the widget.
     */
    AdminSelectOptionsResponse resolve(
            String modelName,
            String fieldName,
            String value
    ) throws UnknownModelException;
}
