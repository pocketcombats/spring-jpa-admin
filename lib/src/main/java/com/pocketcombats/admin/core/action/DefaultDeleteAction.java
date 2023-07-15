package com.pocketcombats.admin.core.action;

import com.pocketcombats.admin.core.AdminRegisteredModel;
import com.pocketcombats.admin.core.RegisteredEntityDetails;
import com.pocketcombats.admin.history.AdminHistoryWriter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.Root;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Order
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
    public boolean isLocalized() {
        return true;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void run(EntityManager em, AdminRegisteredModel model, List<?> entities) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Performing delete action for {} entities", entities.size());
        }
        recordHistory(model, entities);

        // Perform batch delete rather than deleting entities one-by-one
        RegisteredEntityDetails entityDetails = model.entityDetails();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaDelete query = cb.createCriteriaDelete(entityDetails.entityClass());
        Root<?> root = query.from(entityDetails.entityClass());
        query.where(root.in(entities));
        em.createQuery(query).executeUpdate();
    }

    protected void recordHistory(AdminRegisteredModel model, List<?> entities) {
        historyWriter.record(model, "delete", entities);
    }
}
