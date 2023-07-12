package com.pocketcombats.admin.data.action;

import java.io.Serializable;

public record ActionColumn(
        String label,
        boolean bool
) implements Serializable {

}
