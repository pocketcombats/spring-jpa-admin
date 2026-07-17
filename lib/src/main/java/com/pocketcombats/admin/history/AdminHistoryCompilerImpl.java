package com.pocketcombats.admin.history;

import com.pocketcombats.admin.core.AdminModelRegistry;
import com.pocketcombats.admin.core.AdminRegisteredModel;
import com.pocketcombats.admin.core.UnknownModelException;
import com.pocketcombats.admin.core.permission.AdminPermissionService;
import com.pocketcombats.admin.data.history.HistoryEntry;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.jspecify.annotations.Nullable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

public class AdminHistoryCompilerImpl implements AdminHistoryCompiler {

    private final AdminModelRegistry adminModelRegistry;
    private final AdminPermissionService permissionService;
    private final EntityManager em;

    public AdminHistoryCompilerImpl(
            AdminModelRegistry adminModelRegistry,
            AdminPermissionService permissionService,
            EntityManager em
    ) {
        this.adminModelRegistry = adminModelRegistry;
        this.permissionService = permissionService;
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
        // Entries of models the current user cannot view are dropped after fetching,
        // so the compiled log may contain fewer than `size` entries.
        return logs.stream()
                .map(this::toHistoryEntry)
                .filter(Objects::nonNull)
                .toList();
    }

    private @Nullable HistoryEntry toHistoryEntry(AdminHistoryLog record) {
        String modelLabel;
        try {
            AdminRegisteredModel model = adminModelRegistry.resolve(record.getModel());
            if (!permissionService.canView(model)) {
                return null;
            }
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
