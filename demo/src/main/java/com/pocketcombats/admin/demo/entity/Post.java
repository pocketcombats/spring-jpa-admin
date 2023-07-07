package com.pocketcombats.admin.demo.entity;

import jakarta.persistence.*;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "demo_post")
public class Post implements Serializable {

    @Id
    @Column(name = "id", updatable = false)
    private Long id;

    @Column(name = "post_time", nullable = false)
    private Instant postTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private DemoUser author;

    @Column(name = "text", nullable = false)
    private String text;

    @Column(name = "category")
    @Enumerated(EnumType.STRING)
    private Category category;

    @Column(name = "approved", nullable = false)
    private boolean approved;

    @OneToMany(orphanRemoval = true)
    private List<Comment> comments;

    @ManyToMany
    @JoinTable(
            name = "post_tags",
            joinColumns = @JoinColumn(name = "post_id", referencedColumnName = "id", nullable = false, updatable = false),
            inverseJoinColumns = @JoinColumn(name = "tag_id", referencedColumnName = "id", nullable = false, updatable = false)
    )
    private Set<Tag> tags;

    @OneToMany(orphanRemoval = true)
    private List<PostReaction> reactions;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Instant getPostTime() {
        return postTime;
    }

    public void setPostTime(Instant postTime) {
        this.postTime = postTime;
    }

    public DemoUser getAuthor() {
        return author;
    }

    public void setAuthor(DemoUser author) {
        this.author = author;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public boolean isApproved() {
        return approved;
    }

    public void setApproved(boolean approved) {
        this.approved = approved;
    }

    public List<Comment> getComments() {
        return comments;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }

    public Set<Tag> getTags() {
        return tags;
    }

    public void setTags(Set<Tag> tags) {
        this.tags = tags;
    }

    public List<PostReaction> getReactions() {
        return reactions;
    }

    public void setReactions(List<PostReaction> reactions) {
        this.reactions = reactions;
    }
}
