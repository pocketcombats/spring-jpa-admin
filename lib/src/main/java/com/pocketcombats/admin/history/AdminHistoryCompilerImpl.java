package com.pocketcombats.admin.history;

import com.pocketcombats.admin.core.AdminModelRegistry;
import com.pocketcombats.admin.core.AdminRegisteredModel;
import com.pocketcombats.admin.core.UnknownModelException;
import com.pocketcombats.admin.data.history.HistoryEntry;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public class AdminHistoryCompilerImpl implements AdminHistoryCompiler {

    private final AdminModelRegistry adminModelRegistry;
    private final EntityManager em;

    public AdminHistoryCompilerImpl(AdminModelRegistry adminModelRegistry, EntityManager em) {
        this.adminModelRegistry = adminModelRegistry;
        this.em = em;
    }

    @Override
    @Transactional(readOnly = true)
    public List<HistoryEntry> compileLog(int size) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<AdminHistoryLog> query = cb.createQuery(AdminHistoryLog.class);
        Root<AdminHistoryLog> root = query.from(AdminHistoryLog.class);
        query.orderBy(cb.desc(root.get("time")));
        List<AdminHistoryLog> logs = em.createQuery(query)
                .setMaxResults(size)
                .getResultList();
        return logs.stream()
                .map(this::toHistoryEntry)
                .toList();
    }

    private HistoryEntry toHistoryEntry(AdminHistoryLog record) {
        String modelLabel;
        try {
            AdminRegisteredModel model = adminModelRegistry.resolve(record.getModel());
            modelLabel = model.label();
        } catch (UnknownModelException e) {
            // Model could be removed after the history was recorded
            modelLabel = record.getModel();
        }
        return new HistoryEntry(
                record.getAction(),
                record.getModel(),
                modelLabel,
                record.getEntityId(),
                record.getEntityRepresentation(),
                record.getUsername()
        );
    }
}
