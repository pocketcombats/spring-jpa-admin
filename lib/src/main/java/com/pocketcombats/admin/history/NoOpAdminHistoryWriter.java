package com.pocketcombats.admin.history;

import com.pocketcombats.admin.core.AdminRegisteredModel;

import java.util.Collection;

public class NoOpAdminHistoryWriter implements AdminHistoryWriter {

    @Override
    public void record(AdminRegisteredModel model, String action, Collection<?> entities) {

    }

    @Override
    public void record(AdminRegisteredModel model, String action, Object entity) {

    }
}
