package com.pocketcombats.admin.data.action;

import java.io.Serializable;
import java.util.List;

public class ActionRequest implements Serializable {

    private String action;
    private List<String> id;

    public ActionRequest() {
    }

    public ActionRequest(String action, List<String> stringIds) {
        this.action = action;
        this.id = stringIds;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public List<String> getId() {
        return id;
    }

    public void setId(List<String> id) {
        this.id = id;
    }
}
