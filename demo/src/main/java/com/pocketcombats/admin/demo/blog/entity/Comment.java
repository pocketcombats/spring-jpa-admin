package com.pocketcombats.admin.demo.blog.entity;

import com.pocketcombats.admin.AdminField;
import com.pocketcombats.admin.AdminModel;
import com.pocketcombats.admin.demo.user.entity.DemoUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "demo_comment")
@AdminModel
public class Comment implements Serializable {

    @Id
    @Column(name = "id", updatable = false)
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false, updatable = false)
    @AdminField(rawId = true, sortBy = "postTime", representation = "#abbreviate(text, 20)")
    private Post post;

    @Column(name = "post_time", nullable = false, updatable = false)
    @AdminField(sortable = true)
    private Instant postTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false, updatable = false)
    @AdminField(representation = "username")
    private DemoUser author;

    @Column(name = "text", nullable = false)
    @NotBlank
    private String text;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Post getPost() {
        return post;
    }

    public void setPost(Post post) {
        this.post = post;
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
}
