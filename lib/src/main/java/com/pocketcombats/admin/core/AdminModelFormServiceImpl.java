package com.pocketcombats.admin.core;

import com.pocketcombats.admin.data.form.AdminFormField;
import com.pocketcombats.admin.data.form.AdminFormFieldGroup;
import com.pocketcombats.admin.data.form.EntityDetails;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.EntityType;
import jakarta.transaction.Transactional;
import org.springframework.core.convert.ConversionService;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

import java.util.List;
import java.util.Map;

public class AdminModelFormServiceImpl implements AdminModelFormService {

    private final AdminModelRegistry modelRegistry;
    private final EntityManager em;
    private final ConversionService conversionService;

    public AdminModelFormServiceImpl(
            AdminModelRegistry modelRegistry,
            EntityManager em,
            ConversionService conversionService
    ) {
        this.modelRegistry = modelRegistry;
        this.em = em;
        this.conversionService = conversionService;
    }

    private String resolveId(Object entity) {
        Object identifier = em.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(entity);
        return conversionService.convert(identifier, String.class);
    }

    @Override
    @Transactional
    public EntityDetails details(String modelName, String stringId) throws UnknownModelException {
        AdminRegisteredModel model = modelRegistry.resolve(modelName);
        Object entity = findEntity(model, stringId);

        return getEntityDetails(model, entity);
    }

    private EntityDetails getEntityDetails(AdminRegisteredModel model, Object entity) {
        List<AdminFormFieldGroup> formFieldGroups = mapFieldGroups(model.fieldsets(), FormAction.UPDATE, entity);
        return new EntityDetails(model.modelName(), resolveId(entity), formFieldGroups);
    }

    private Object findEntity(AdminRegisteredModel model, String stringId) {
        EntityType<?> entityTypeDescriptor = em.getEntityManagerFactory().getMetamodel().entity(model.entityClass());
        Class<?> idType = entityTypeDescriptor.getIdType().getJavaType();// This should be cached probably?

        Object id = conversionService.convert(stringId, idType);
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<?> q = cb.createQuery(model.entityClass());
        Root<?> root = q.from(model.entityClass());
        q.where(
                cb.equal(root.get(entityTypeDescriptor.getId(idType).getName()), id)
                // TODO: support additional predicates from config
        );
        List<?> resultList = em.createQuery(q).setMaxResults(2).getResultList();
        // TODO: Handle 0 and 2 result list size
        Object entity = resultList.get(0);
        return entity;
    }

    @Deprecated
    private List<AdminFormFieldGroup> mapFieldGroups(
            List<AdminModelFieldset> fieldsets,
            FormAction action,
            Object entity
    ) {
        return fieldsets.stream()
                .map(fieldset -> new AdminFormFieldGroup(
                        fieldset.label(),
                        null,
                        fieldset.fields().stream()
                                .map(field -> new AdminFormField(
                                        field.name(),
                                        field.label(),
                                        !isEditable(field, action),
                                        field.template(),
                                        field.valueAccessor().readValue(entity),
                                        field.valueAccessor().getModelAttributes(em)
                                ))
                                .toList()
                ))
                .toList();
    }

    private static boolean isEditable(AdminModelField field, FormAction action) {
        return switch (action) {
            case UPDATE -> field.updatable();
            case CREATE -> field.insertable();
        };
    }

    @Override
    @Transactional
    public AdminModelEditingResult update(String modelName, String stringId, Map<String, String> rawData) throws UnknownModelException {
        AdminRegisteredModel model = modelRegistry.resolve(modelName);
        Object entity = findEntity(model, stringId);

        List<AdminModelField> writeableFields = model.fieldsets().stream()
                .flatMap(fieldset -> fieldset.fields().stream())
                .filter(AdminModelField::updatable)
                .toList();

        BindingResult bindingResult = new BeanPropertyBindingResult(entity, modelName);
        for (AdminModelField field : writeableFields) {
            field.valueAccessor().setValue(entity, rawData.get("model-field-" + field.name()), bindingResult);
        }

        return new AdminModelEditingResult(
                getEntityDetails(model, entity),
                bindingResult
        );
    }
}
