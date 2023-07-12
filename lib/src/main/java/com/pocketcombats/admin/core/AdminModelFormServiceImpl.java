package com.pocketcombats.admin.core;

import com.pocketcombats.admin.core.field.AdminFormFieldPluralValueAccessor;
import com.pocketcombats.admin.core.field.AdminFormFieldSingularValueAccessor;
import com.pocketcombats.admin.core.field.AdminFormFieldValueAccessor;
import com.pocketcombats.admin.data.form.AdminFormField;
import com.pocketcombats.admin.data.form.AdminFormFieldGroup;
import com.pocketcombats.admin.data.form.EntityDetails;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

import java.util.List;

public class AdminModelFormServiceImpl implements AdminModelFormService {

    private static final Logger LOG = LoggerFactory.getLogger(AdminModelFormServiceImpl.class);

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
        List<AdminFormFieldGroup> formFieldGroups = mapFieldGroups(model, FormAction.UPDATE, entity);
        return new EntityDetails(
                model.modelName(),
                resolveId(entity),
                model.label(),
                formFieldGroups,
                model.actions().containsKey("delete")
        );
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Object findEntity(AdminRegisteredModel model, String stringId) {
        RegisteredEntityDetails entityDetails = model.entityDetails();
        Object id = conversionService.convert(stringId, entityDetails.idAttribute().getJavaType());
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<?> q = cb.createQuery(entityDetails.entityClass());
        Root<?> root = q.from(entityDetails.entityClass());
        q.where(
                cb.equal(root.get((SingularAttribute) entityDetails.idAttribute()), id)
                // TODO: support additional predicates from config
        );
        List<?> resultList = em.createQuery(q).setMaxResults(2).getResultList();
        // TODO: Handle 0 and 2 result list size
        Object entity = resultList.get(0);
        return entity;
    }

    @Deprecated
    private List<AdminFormFieldGroup> mapFieldGroups(
            AdminRegisteredModel model,
            FormAction action,
            Object entity
    ) {
        return model.fieldsets().stream()
                .map(fieldset -> new AdminFormFieldGroup(
                        fieldset.label(),
                        null,
                        fieldset.fields().stream()
                                .map(field -> new AdminFormField(
                                        field.name(),
                                        field.label(),
                                        !isEditable(model, field, action),
                                        field.template(),
                                        field.valueAccessor().readValue(entity),
                                        field.valueAccessor().getModelAttributes()
                                ))
                                .toList()
                ))
                .toList();
    }

    private static boolean isEditable(AdminRegisteredModel model, AdminModelField field, FormAction action) {
        return switch (action) {
            case UPDATE -> model.updatable() && field.updatable();
            case CREATE -> field.insertable();
        };
    }

    @Override
    @Transactional
    public AdminModelEditingResult update(
            String modelName,
            String stringId,
            MultiValueMap<String, String> rawData
    ) throws UnknownModelException {
        AdminRegisteredModel model = modelRegistry.resolve(modelName);
        Object entity = findEntity(model, stringId);

        List<AdminModelField> writeableFields = model.fieldsets().stream()
                .flatMap(fieldset -> fieldset.fields().stream())
                .filter(AdminModelField::updatable)
                .toList();

        BindingResult bindingResult = new BeanPropertyBindingResult(entity, modelName);
        for (AdminModelField field : writeableFields) {
            AdminFormFieldValueAccessor accessor = field.valueAccessor();
            if (accessor instanceof AdminFormFieldSingularValueAccessor singularValueAccessor) {
                singularValueAccessor.setValue(entity, rawData.getFirst("model-field-" + field.name()), bindingResult);
            } else if (accessor instanceof AdminFormFieldPluralValueAccessor pluralValueAccessor) {
                pluralValueAccessor.setValues(entity, rawData.get("model-field-" + field.name()), bindingResult);
            } else {
                LOG.error("Can't resolve value accessor type for field {} of model {}", field.name(), modelName);
            }
        }

        return new AdminModelEditingResult(
                getEntityDetails(model, entity),
                bindingResult
        );
    }
}
