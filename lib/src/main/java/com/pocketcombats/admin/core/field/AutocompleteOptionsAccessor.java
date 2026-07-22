package com.pocketcombats.admin.core.field;

import com.pocketcombats.admin.data.form.AdminSelectOption;
import com.pocketcombats.admin.data.form.AdminSelectOptionsResponse;
import org.jspecify.annotations.Nullable;

/**
 * Capability of a form field accessor to serve paginated options for the autocomplete widget.
 */
public interface AutocompleteOptionsAccessor {

    /**
     * Whether this field actually renders as an autocomplete and may be served by the options
     * endpoint.
     * Implementations return {@code false} for fields that keep the preloaded values, so the
     * endpoint stays consistent with the rendered widget.
     */
    default boolean autocompleteSupported() {
        return true;
    }

    /**
     * Collects a single page of options, optionally filtered by a search query.
     *
     * @param query    user-typed search text, or {@code null}/blank for no text filtering
     * @param page     1-based page number
     * @param pageSize maximum options per page
     */
    AdminSelectOptionsResponse collectOptions(
            @Nullable String query,
            int page,
            int pageSize
    );

    /**
     * Resolves a single option for the given submit-ready value, used to prefill the current selection.
     *
     * @return the option, or {@code null} when the value is empty or the entity no longer exists
     */
    @Nullable AdminSelectOption resolveOption(String value);
}
