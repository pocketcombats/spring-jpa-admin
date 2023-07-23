package com.pocketcombats.admin.data.list;

import java.io.Serializable;

public record EntityRelation(
        String model,
        String id
) implements Serializable {
}
