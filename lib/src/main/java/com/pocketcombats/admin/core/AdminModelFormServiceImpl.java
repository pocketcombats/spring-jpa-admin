package com.pocketcombats.admin.core;

import com.pocketcombats.admin.core.field.AdminFormFieldPluralValueAccessor;
import com.pocketcombats.admin.core.field.AdminFormFieldSingularValueAccessor;
import com.pocketcombats.admin.core.field.AdminFormFieldValueAccessor;
import com.pocketcombats.admin.data.form.AdminFormField;
import com.pocketcombats.admin.data.form.AdminFormFieldGroup;
import com.pocketcombats.admin.data.form.EntityDetails;
import com.pocketcombats.admin.history.AdminHistoryWriter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.SingularAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Validator;

import java.util.List;

public class AdminModelFormServiceImpl implements AdminModelFormService {

    private static final Logger LOG = LoggerFactory.getLogger(AdminModelFormServiceImpl.class);

    private final AdminModelRegistry modelRegistry;
    private final AdminHistoryWriter historyWriter;
    private final EntityManager em;
    private final Validator validator;
    private final ConversionService conversionService;

    public AdminModelFormServiceImpl(
            AdminModelRegistry modelRegistry,
            AdminHistoryWriter historyWriter,
            EntityManager em,
            Validator validator,
            ConversionService conversionService
    ) {
        this.modelRegistry = modelRegistry;
        this.historyWriter = historyWriter;
        this.em = em;
        this.validator = validator;
        this.conversionService = conversionService;
    }

    private String resolveId(Object entity) {
        Object identifier = em.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(entity);
        return conversionService.convert(identifier, String.class);
    }

    @Override
    @Transactional(readOnly = true)
    public EntityDetails details(String modelName, String stringId) throws UnknownModelException {
        AdminRegisteredModel model = modelRegistry.resolve(modelName);
        Object entity = findEntity(model, stringId);

        return getEntityDetails(model, entity, FormAction.UPDATE);
    }

    private EntityDetails getEntityDetails(AdminRegisteredModel model, Object entity, FormAction action) {
        List<AdminFormFieldGroup> formFieldGroups = mapFieldGroups(model, entity, action);
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

    private List<AdminFormFieldGroup> mapFieldGroups(
            AdminRegisteredModel model,
            Object entity,
            FormAction action
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
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public AdminModelEditingResult update(
            String modelName,
            String stringId,
            MultiValueMap<String, String> rawData
    ) throws UnknownModelException {
        AdminRegisteredModel model = modelRegistry.resolve(modelName);
        Object entity = findEntity(model, stringId);
        historyWriter.record(model, "edit", entity);

        BindingResult bindingResult = bind(model, entity, FormAction.UPDATE, rawData);
        if (bindingResult.hasErrors()) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        }

        return new AdminModelEditingResult(
                getEntityDetails(model, entity, FormAction.UPDATE),
                bindingResult
        );
    }

    private BindingResult bind(
            AdminRegisteredModel model,
            Object entity,
            FormAction action,
            MultiValueMap<String, String> rawData
    ) {
        List<AdminModelField> writeableFields = model.fieldsets().stream()
                .flatMap(fieldset -> fieldset.fields().stream())
                .filter(field -> isEditable(model, field, action))
                .toList();

        BindingResult bindingResult = new BeanPropertyBindingResult(entity, model.modelName());
        for (AdminModelField field : writeableFields) {
            AdminFormFieldValueAccessor accessor = field.valueAccessor();
            if (accessor instanceof AdminFormFieldSingularValueAccessor singularValueAccessor) {
                singularValueAccessor.setValue(entity, rawData.getFirst("model-field-" + field.name()), bindingResult);
            } else if (accessor instanceof AdminFormFieldPluralValueAccessor pluralValueAccessor) {
                pluralValueAccessor.setValues(entity, rawData.get("model-field-" + field.name()), bindingResult);
            } else {
                LOG.error("Can't resolve value accessor type for field {} of model {}", field.name(), model.modelName());
            }
        }
        validator.validate(entity, bindingResult);
        return bindingResult;
    }

    @Override
    @Transactional(readOnly = true)
    public EntityDetails create(String modelName) throws UnknownModelException {
        AdminRegisteredModel model = modelRegistry.resolve(modelName);
        Object entity = BeanUtils.instantiateClass(model.entityDetails().entityClass());

        return getEntityDetails(model, entity, FormAction.CREATE);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public AdminModelEditingResult create(
            String modelName,
            MultiValueMap<String, String> rawData
    ) throws UnknownModelException {
        AdminRegisteredModel model = modelRegistry.resolve(modelName);
        Object entity = BeanUtils.instantiateClass(model.entityDetails().entityClass());

        BindingResult bindingResult = bind(model, entity, FormAction.CREATE, rawData);
        if (!bindingResult.hasErrors()) {
            em.persist(entity);
            historyWriter.record(model, "create", entity);
        } else {
            LOG.debug("Binding result has errors, can't save new {}", model.modelName());
        }

        return new AdminModelEditingResult(
                getEntityDetails(model, entity, FormAction.CREATE),
                bindingResult
        );
    }
}
