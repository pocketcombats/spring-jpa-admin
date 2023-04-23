package com.pocketcombats.admin.data.list;

import java.io.Serializable;
import java.util.List;

public class AdminEntityListEntry implements Serializable {

    private final String id;
    private final List<Object> attributes;

    public AdminEntityListEntry(String id, List<Object> attributes) {
        this.id = id;
        this.attributes = attributes;
    }

    public String getId() {
        return id;
    }

    public Object getAttributeByIndex(int index) {
        return attributes.get(index);
    }
}
