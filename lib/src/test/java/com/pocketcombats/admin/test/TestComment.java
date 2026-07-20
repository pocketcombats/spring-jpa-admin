package com.pocketcombats.admin.test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

/**
 * Comment on a {@link TestPost}: two to-one hops away from {@link TestCategory}
 * (comment → post → category), with a primitive counter and a to-one relation
 * to the composite-id {@link TestCompositeTag}.
 */
@Entity
public class TestComment {

    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private TestPost post;

    @ManyToOne(fetch = FetchType.LAZY)
    private TestCompositeTag tag;

    private int likes;

    // Setter-only bean property: readers must fall back to field access (no getter on purpose).
    private String moderationNote;

    protected TestComment() {
    }

    public TestComment(Long id, TestPost post) {
        this.id = id;
        this.post = post;
    }

    public TestComment(Long id, TestPost post, int likes) {
        this.id = id;
        this.post = post;
        this.likes = likes;
    }

    public Long getId() {
        return id;
    }

    public TestPost getPost() {
        return post;
    }

    public void setPost(TestPost post) {
        this.post = post;
    }

    public TestCompositeTag getTag() {
        return tag;
    }

    public void setTag(TestCompositeTag tag) {
        this.tag = tag;
    }

    public int getLikes() {
        return likes;
    }

    public void setLikes(int likes) {
        this.likes = likes;
    }

    public void setModerationNote(String moderationNote) {
        this.moderationNote = moderationNote;
    }
}
