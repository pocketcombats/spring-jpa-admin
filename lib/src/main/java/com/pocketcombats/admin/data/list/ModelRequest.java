package com.pocketcombats.admin.data.list;

import org.jspecify.annotations.Nullable;

import java.io.Serializable;

public class ModelRequest implements Serializable {

    private @Nullable String search;
    private @Nullable String sort;
    private @Nullable Integer page;

    public @Nullable String getSearch() {
        return search;
    }

    public void setSearch(@Nullable String search) {
        this.search = search;
    }

    public @Nullable String getSort() {
        return sort;
    }

    public void setSort(@Nullable String sort) {
        this.sort = sort;
    }

    public @Nullable Integer getPage() {
        return page;
    }

    public void setPage(@Nullable Integer page) {
        this.page = page;
    }
}
