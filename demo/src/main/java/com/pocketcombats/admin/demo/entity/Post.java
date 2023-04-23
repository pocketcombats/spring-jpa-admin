package com.pocketcombats.admin.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

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

    @Column(name = "approved", nullable = false)
    private boolean approved;

    @OneToMany(orphanRemoval = true)
    private List<Comment> comments;

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

    public List<PostReaction> getReactions() {
        return reactions;
    }

    public void setReactions(List<PostReaction> reactions) {
        this.reactions = reactions;
    }
}
