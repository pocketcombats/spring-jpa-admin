package com.pocketcombats.admin.core.action;

import com.pocketcombats.admin.core.AdminRegisteredModel;
import com.pocketcombats.admin.history.AdminHistoryWriter;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Default "delete" action. Removes each entity via {@link EntityManager#remove}, so JPA cascade
 * {@code REMOVE}, {@code orphanRemoval} and entity lifecycle callbacks (e.g. {@code @PreRemove}) apply.
 */
public class DefaultDeleteAction implements AdminModelAction {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultDeleteAction.class);

    private final AdminHistoryWriter historyWriter;

    public DefaultDeleteAction(AdminHistoryWriter historyWriter) {
        this.historyWriter = historyWriter;
    }

    @Override
    public String getId() {
        return "delete";
    }

    @Override
    public String getLabel() {
        return "spring-jpa-admin.action.delete";
    }

    @Override
    public String getDescription() {
        return "spring-jpa-admin.action.delete.confirmation";
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public void run(EntityManager em, AdminRegisteredModel model, List<?> entities) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Performing delete action for {} entities", entities.size());
        }
        recordHistory(model, entities);

        // The entities are already managed (loaded by the action service); em.remove is what
        // lets cascades and lifecycle callbacks run, unlike a bulk CriteriaDelete
        for (Object entity : entities) {
            em.remove(entity);
        }
    }

    protected void recordHistory(AdminRegisteredModel model, List<?> entities) {
        historyWriter.record(model, "delete", entities);
    }
}
