package com.pocketcombats.admin.data.action;

import java.io.Serializable;
import java.util.List;

public record ActionRequest(
        String action,
        List<String> id
) implements Serializable {
}
