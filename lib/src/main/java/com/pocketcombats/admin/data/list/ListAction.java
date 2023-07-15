package com.pocketcombats.admin.data.list;

import java.io.Serializable;

public record ListAction(
        String id,
        String label
) implements Serializable {

}
