package com.pocketcombats.admin.data;

import java.io.Serializable;

public record AdminModelInfo(
        String label,
        String modelName
) implements Serializable {

}
