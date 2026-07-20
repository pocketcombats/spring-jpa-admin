package com.pocketcombats.admin.test;

import java.io.Serializable;
import java.util.Objects;

/** {@code @IdClass} of {@link TestCompositeTag}. */
public class TestCompositeTagId implements Serializable {

    private String namespace;
    private String name;

    public TestCompositeTagId() {
    }

    public TestCompositeTagId(String namespace, String name) {
        this.namespace = namespace;
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TestCompositeTagId other)) {
            return false;
        }
        return Objects.equals(namespace, other.namespace) && Objects.equals(name, other.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespace, name);
    }

    @Override
    public String toString() {
        return namespace + ":" + name;
    }
}
