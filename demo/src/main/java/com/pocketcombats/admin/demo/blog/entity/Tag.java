package com.pocketcombats.admin.demo.blog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.io.Serializable;

@Entity
@Table(name = "tag")
public class Tag implements Serializable {

    @Id
    @Column(name = "id", updatable = false)
    private Long id;

    @Column(name = "text", nullable = false, updatable = false)
    private String text;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
