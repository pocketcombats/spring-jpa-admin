package com.pocketcombats.admin.data.form;

import java.io.Serializable;

/**
 * A single selectable option for a {@link com.pocketcombats.admin.core.field.ToOneFormFieldAccessor to-one field}
 * autocomplete widget.
 *
 * @param id   entity id in its submit-ready string form (carries the widget id prefix)
 * @param text human-readable representation of the entity
 */
public record AdminSelectOption(
        String id,
        String text
) implements Serializable {

}
