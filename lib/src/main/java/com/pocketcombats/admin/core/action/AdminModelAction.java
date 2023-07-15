package com.pocketcombats.admin.core.action;

import com.pocketcombats.admin.core.AdminRegisteredModel;
import jakarta.persistence.EntityManager;

import java.util.List;

public interface AdminModelAction {

    String getId();

    String getLabel();

    String getDescription();

    void run(EntityManager em, AdminRegisteredModel model, List<?> entities);
}
