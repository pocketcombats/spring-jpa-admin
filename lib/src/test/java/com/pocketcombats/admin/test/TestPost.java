package com.pocketcombats.admin.test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import java.time.Instant;

@Entity
public class TestPost {

    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private TestCategory category;

    private Instant postTime;

    protected TestPost() {
    }

    public TestPost(Long id) {
        this.id = id;
    }

    public TestPost(Long id, TestCategory category) {
        this.id = id;
        this.category = category;
    }

    public Long getId() {
        return id;
    }

    public TestCategory getCategory() {
        return category;
    }

    public void setCategory(TestCategory category) {
        this.category = category;
    }

    public Instant getPostTime() {
        return postTime;
    }

    public void setPostTime(Instant postTime) {
        this.postTime = postTime;
    }
}
