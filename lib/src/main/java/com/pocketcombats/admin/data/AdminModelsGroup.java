package com.pocketcombats.admin.data;

import java.io.Serializable;
import java.util.List;

public record AdminModelsGroup(
        String label,
        List<AdminModelInfo> models
) implements Serializable {

}
