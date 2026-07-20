package com.pocketcombats.admin.test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;

/** Entity with a composite ({@code @IdClass}) identifier. */
@Entity
@IdClass(TestCompositeTagId.class)
public class TestCompositeTag {

    @Id
    private String namespace;

    @Id
    private String name;

    protected TestCompositeTag() {
    }

    public TestCompositeTag(String namespace, String name) {
        this.namespace = namespace;
        this.name = name;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return namespace + ":" + name;
    }
}
