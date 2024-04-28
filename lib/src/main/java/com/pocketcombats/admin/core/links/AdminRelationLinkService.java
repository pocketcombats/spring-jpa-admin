package com.pocketcombats.admin.core.links;

import com.pocketcombats.admin.core.AdminModelListEntityMapper;
import com.pocketcombats.admin.core.AdminModelListField;
import com.pocketcombats.admin.core.AdminModelRegistry;
import com.pocketcombats.admin.core.AdminRegisteredModel;
import com.pocketcombats.admin.core.UnknownModelException;
import com.pocketcombats.admin.core.formatter.ValueFormatter;
import com.pocketcombats.admin.data.form.AdminRelationLink;
import com.pocketcombats.admin.data.form.AdminRelationPreview;
import com.pocketcombats.admin.data.list.EntityRelation;
import com.pocketcombats.admin.data.list.Parent;
import jakarta.annotation.Nullable;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class AdminRelationLinkService {

    private static final Logger LOG = LoggerFactory.getLogger(AdminRelationLinkService.class);

    private final AdminModelRegistry modelRegistry;
    private final AdminModelListEntityMapper mapper;
    private final EntityManager em;
    private final ConversionService conversionService;

    public AdminRelationLinkService(
            AdminModelRegistry modelRegistry,
            AdminModelListEntityMapper mapper,
            EntityManager em,
            ConversionService conversionService
    ) {
        this.modelRegistry = modelRegistry;
        this.mapper = mapper;
        this.em = em;
        this.conversionService = conversionService;
    }

    public List<AdminRelationLink> collectRelationLinks(AdminRegisteredModel model, Object entity) {
        return model.links().stream()
                .map(adminModelLink -> createRelationLink(entity, adminModelLink))
                .filter(Objects::nonNull)
                .toList();
    }

    @Nullable
    private AdminRelationLink createRelationLink(Object entity, AdminModelLink modelLink) {
        AdminRegisteredModel targetModel;
        try {
            targetModel = modelRegistry.resolve(modelLink.target().getSimpleName());
        } catch (UnknownModelException e) {
            LOG.error("Class {} isn't a registered admin model", modelLink.target());
            return null;
        }
        String label = modelLink.label();
        if (StringUtils.isEmpty(label)) {
            label = targetModel.label();
        }
        return new AdminRelationLink(
                label,
                targetModel.modelName(),
                createRelationPreviews(targetModel, modelLink, entity)
        );
    }

    private List<AdminRelationPreview> createRelationPreviews(
            AdminRegisteredModel model,
            AdminModelLink modelLink,
            Object ref
    ) {
        if (modelLink.preview() < 1) {
            return Collections.emptyList();
        }

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<?> query = cb.createQuery(modelLink.target());
        Root<?> root = query.from(modelLink.target());
        Predicate predicate = modelLink.predicateFactory().createPredicate(ref, root);
        query.where(predicate);
        if (modelLink.orderFactory() != null) {
            query.orderBy(modelLink.orderFactory().create(root));
        }
        List<?> resultList = em.createQuery(query)
                .setMaxResults(modelLink.preview())
                .getResultList();
        return resultList.stream()
                .map(entity -> createPreview(model, entity, modelLink.formatter()))
                .toList();
    }

    private AdminRelationPreview createPreview(
            AdminRegisteredModel model,
            Object entity,
            @Nullable ValueFormatter formatter
    ) {
        if (formatter != null) {
            return new AdminRelationPreview(resolveId(entity), formatter.format(entity));
        } else {
            AdminModelListField firstListField = model.listFields().get(0);
            Object representation = mapper.fieldValue(firstListField, entity);
            return new AdminRelationPreview(resolveId(entity), representation.toString());
        }
    }

    private String resolveId(Object entity) {
        Object identifier = em.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(entity);
        return conversionService.convert(identifier, String.class);
    }

    @Transactional(readOnly = true)
    public Parent getParentInfo(EntityRelation relation) throws UnknownModelException {
        AdminRegisteredModel model = modelRegistry.resolve(relation.model());
        Object id = conversionService.convert(
                relation.id(),
                model.entityDetails().idAttribute().getJavaType()
        );
        Object entity = em.find(model.entityDetails().entityClass(), id);
        AdminRelationPreview preview = createPreview(model, entity, null);
        return new Parent(model.label(), model.modelName(), preview);
    }
}
