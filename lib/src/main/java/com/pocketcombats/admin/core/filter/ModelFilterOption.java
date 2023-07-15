package com.pocketcombats.admin.core.filter;

import java.io.Serializable;

public record ModelFilterOption(
        String label,
        String value
) implements Serializable {

}
