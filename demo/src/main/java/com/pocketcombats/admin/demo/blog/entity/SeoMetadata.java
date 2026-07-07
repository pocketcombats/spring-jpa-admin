package com.pocketcombats.admin.demo.blog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.jspecify.annotations.Nullable;

import java.io.Serializable;

/**
 * Demonstrates a JPA {@code @Embeddable} rendered as a group of nested form inputs.
 */
@Embeddable
public class SeoMetadata implements Serializable {

    @Column(name = "seo_title")
    private String metaTitle;

    @Column(name = "seo_description")
    private String metaDescription;

    public @Nullable String getMetaTitle() {
        return metaTitle;
    }

    public void setMetaTitle(@Nullable String metaTitle) {
        this.metaTitle = metaTitle;
    }

    public @Nullable String getMetaDescription() {
        return metaDescription;
    }

    public void setMetaDescription(@Nullable String metaDescription) {
        this.metaDescription = metaDescription;
    }
}
