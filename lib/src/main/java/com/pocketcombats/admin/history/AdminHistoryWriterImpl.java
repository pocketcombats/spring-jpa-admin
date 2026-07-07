package com.pocketcombats.admin.history;

import com.pocketcombats.admin.core.AdminModelListEntityMapper;
import com.pocketcombats.admin.core.AdminModelListField;
import com.pocketcombats.admin.core.AdminRegisteredModel;
import com.pocketcombats.admin.util.EntityUtils;
import jakarta.persistence.EntityManager;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class AdminHistoryWriterImpl implements AdminHistoryWriter {

    private static final int MAX_REPR_LENGTH = 200;
    private static final String UNKNOWN_USERNAME = "unknown";

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
                    Objects.toString(mapper.fieldValue(representationField, entity), ""),
                    MAX_REPR_LENGTH
            );

            AdminHistoryLog log = new AdminHistoryLog();
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
        return EntityUtils.getEntityStringId(em, conversionService, entity);
    }

    protected String resolveUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return UNKNOWN_USERNAME;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        return principal == null ? UNKNOWN_USERNAME : principal.toString();
    }
}
