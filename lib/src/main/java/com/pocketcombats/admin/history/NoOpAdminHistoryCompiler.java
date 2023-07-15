package com.pocketcombats.admin.history;

import com.pocketcombats.admin.data.history.HistoryEntry;

import java.util.Collections;
import java.util.List;

public class NoOpAdminHistoryCompiler implements AdminHistoryCompiler {

    @Override
    public List<HistoryEntry> compileLog(int size) {
        return Collections.emptyList();
    }
}
