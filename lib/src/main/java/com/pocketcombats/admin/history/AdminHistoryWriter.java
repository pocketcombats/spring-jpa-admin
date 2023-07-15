package com.pocketcombats.admin.history;

import com.pocketcombats.admin.core.AdminRegisteredModel;

import java.util.Collection;

public interface AdminHistoryWriter {

    void record(AdminRegisteredModel model, String action, Collection<?> entities);

    void record(AdminRegisteredModel model, String action, Object entity);
}
