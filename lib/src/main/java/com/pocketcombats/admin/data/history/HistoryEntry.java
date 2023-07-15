package com.pocketcombats.admin.data.history;

import java.io.Serializable;

public record HistoryEntry(
        String action,
        String model,
        String modelLabel,
        String id,
        String representation,
        String username
) implements Serializable {

}
