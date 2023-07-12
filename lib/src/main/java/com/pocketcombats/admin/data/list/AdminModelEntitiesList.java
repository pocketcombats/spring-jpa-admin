package com.pocketcombats.admin.data.list;

import java.io.Serializable;
import java.util.List;

public record AdminModelEntitiesList(
        String label,
        String modelName,
        boolean searchable,
        boolean insertable,
        int page,
        int pagesCount,
        List<ListFilter> filters,
        List<AdminListColumn> columns,
        List<ListAction> actions,
        List<AdminEntityListEntry> entities
) implements Serializable {

}
