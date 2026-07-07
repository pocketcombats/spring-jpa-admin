package com.pocketcombats.admin.data.list;

import org.jspecify.annotations.Nullable;

import java.io.Serializable;
import java.util.List;

public class AdminEntityListEntry implements Serializable {

    private final String id;
    private final List<@Nullable Object> attributes;

    public AdminEntityListEntry(String id, List<@Nullable Object> attributes) {
        this.id = id;
        this.attributes = attributes;
    }

    public String getId() {
        return id;
    }

    public @Nullable Object getAttributeByIndex(int index) {
        return attributes.get(index);
    }
}
