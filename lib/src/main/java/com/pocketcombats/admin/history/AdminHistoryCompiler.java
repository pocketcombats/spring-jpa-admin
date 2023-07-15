package com.pocketcombats.admin.history;

import com.pocketcombats.admin.data.history.HistoryEntry;

import java.util.List;

public interface AdminHistoryCompiler {

    List<HistoryEntry> compileLog(int size);
}
