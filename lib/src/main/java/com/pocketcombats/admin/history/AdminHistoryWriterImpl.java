package com.pocketcombats.admin.history;

import com.pocketcombats.admin.core.AdminModelListEntityMapper;
import com.pocketcombats.admin.core.AdminModelListField;
import com.pocketcombats.admin.core.AdminRegisteredModel;
import jakarta.persistence.EntityManager;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public class AdminHistoryWriterImpl implements AdminHistoryWriter {

    private static final int MAX_REPR_LENGTH = 200;

    private final AdminModelListEntityMapper mapper;
    private final EntityManager em;
    private final ConversionService conversionService;

    public AdminHistoryWriterImpl(
            AdminModelListEntityMapper mapper,
            EntityManager em,
            ConversionService conversionService
    ) {
        this.mapper = mapper;
        this.em = em;
        this.conversionService = conversionService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void record(AdminRegisteredModel model, String action, Collection<?> entities) {
        Instant now = Instant.now();
        String username = resolveUsername();
        AdminModelListField representationField = model.listFields().get(0);
        for (Object entity : entities) {
            String id = resolveId(entity);
            String representation = StringUtils.abbreviate(
                    mapper.fieldValue(representationField, entity).toString(),
                    MAX_REPR_LENGTH
            );

            AdminHistoryLog log = new AdminHistoryLog();
            log.setAction(action);
            log.setTime(now);
            log.setModel(model.modelName());
            log.setAction(action);
            log.setUsername(username);
            log.setEntityId(id);
            log.setEntityRepresentation(representation);
            em.persist(log);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void record(AdminRegisteredModel model, String action, Object entity) {
        record(model, action, List.of(entity));
    }

    private String resolveId(Object entity) {
        Object identifier = em.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(entity);
        return conversionService.convert(identifier, String.class);
    }

    protected String resolveUsername() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        return principal.toString();
    }
}
