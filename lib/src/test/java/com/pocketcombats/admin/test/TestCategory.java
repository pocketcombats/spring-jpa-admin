package com.pocketcombats.admin.test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

import java.util.List;

@Entity
public class TestCategory {

    @Id
    private Long id;

    private String name;

    @OneToMany(mappedBy = "category")
    private List<TestPost> posts;

    protected TestCategory() {
    }

    public TestCategory(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<TestPost> getPosts() {
        return posts;
    }

    @Override
    public String toString() {
        return name;
    }
}
