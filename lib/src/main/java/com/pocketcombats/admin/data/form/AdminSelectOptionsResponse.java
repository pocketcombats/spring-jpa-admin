package com.pocketcombats.admin.data.form;

import java.io.Serializable;
import java.util.List;

/**
 * A page of {@link AdminSelectOption}s returned by the to-one field options endpoint.
 *
 * @param results options for the requested page
 * @param hasMore whether more results exist beyond this page
 */
public record AdminSelectOptionsResponse(
        List<AdminSelectOption> results,
        boolean hasMore
) implements Serializable {

}
