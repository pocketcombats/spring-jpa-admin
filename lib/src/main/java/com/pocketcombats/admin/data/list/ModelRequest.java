package com.pocketcombats.admin.data.list;

import java.io.Serializable;

public class ModelRequest implements Serializable {

    private String search;
    private String sort;
    private Integer page;

    public String getSearch() {
        return search;
    }

    public void setSearch(String search) {
        this.search = search;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }
}
