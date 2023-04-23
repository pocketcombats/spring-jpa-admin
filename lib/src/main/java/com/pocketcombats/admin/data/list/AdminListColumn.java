package com.pocketcombats.admin.data.list;

import java.io.Serializable;

public record AdminListColumn(
        String name,
        String label,
        boolean bool,
        boolean sortable
) implements Serializable {

}
