package com.pocketcombats.admin.core;

import com.pocketcombats.admin.AdminValidation;
import com.pocketcombats.admin.core.field.AdminFormFieldPluralValueAccessor;
import com.pocketcombats.admin.core.field.AdminFormFieldSingularValueAccessor;
import com.pocketcombats.admin.core.field.AdminFormFieldValueAccessor;
import com.pocketcombats.admin.core.links.AdminRelationLinkService;
import com.pocketcombats.admin.data.form.AdminFormField;
import com.pocketcombats.admin.data.form.AdminFormFieldGroup;
import com.pocketcombats.admin.data.form.AdminRelationLink;
import com.pocketcombats.admin.data.form.EntityDetails;
import com.pocketcombats.admin.history.AdminHistoryWriter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.validation.groups.Default;
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
import org.springframework.validation.SmartValidator;

import java.util.Collections;
import java.util.List;

public class AdminModelFormServiceImpl implements AdminModelFormService {

    private static final Logger LOG = LoggerFactory.getLogger(AdminModelFormServiceImpl.class);

    private final AdminModelRegistry modelRegistry;
    private final AdminHistoryWriter historyWriter;
    private final AdminRelationLinkService relationLinkService;
    private final EntityManager em;
    private final SmartValidator validator;
    private final ConversionService conversionService;

    public AdminModelFormServiceImpl(
            AdminModelRegistry modelRegistry,
            AdminHistoryWriter historyWriter,
            AdminRelationLinkService relationLinkService,
            EntityManager em,
            SmartValidator validator,
            ConversionService conversionService
    ) {
        this.modelRegistry = modelRegistry;
        this.historyWriter = historyWriter;
        this.relationLinkService = relationLinkService;
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
        List<AdminRelationLink> links = action == FormAction.CREATE
                ? Collections.emptyList()
                : relationLinkService.collectRelationLinks(model, entity);
        return new EntityDetails(
                model.modelName(),
                resolveId(entity),
                model.label(),
                formFieldGroups,
                links,
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
                                .filter(field -> action == FormAction.UPDATE || field.insertable())
                                .map(field -> new AdminFormField(
                                        field.name(),
                                        field.label(),
                                        field.description(),
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
        BindingResult bindingResult;
        if (model.updatable()) {
            historyWriter.record(model, "edit", entity);

            bindingResult = bind(model, entity, FormAction.UPDATE, rawData);
            if (bindingResult.hasErrors()) {
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            }
        } else {
            bindingResult = new BeanPropertyBindingResult(entity, model.modelName());
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
        validator.validate(entity, bindingResult, AdminValidation.class, Default.class);
        return bindingResult;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public BindingResult updateField(
            String modelName,
            String stringId,
            String fieldName,
            String value
    ) throws UnknownModelException {
        AdminRegisteredModel model = modelRegistry.resolve(modelName);
        AdminModelField field = model.fieldsets().stream()
                .flatMap(fieldset -> fieldset.fields().stream())
                .filter(candidate -> candidate.name().equals(fieldName))
                .findAny()
                .orElseThrow();
        if (!isEditable(model, field, FormAction.UPDATE)) {
            LOG.error("Model {} field {} is not editable", modelName, fieldName);
            throw new UnknownModelException();
        }
        Object entity = findEntity(model, stringId);
        historyWriter.record(model, "edit " + fieldName, entity);
        BindingResult bindingResult = new BeanPropertyBindingResult(entity, model.modelName());
        AdminFormFieldValueAccessor accessor = field.valueAccessor();
        if (accessor instanceof AdminFormFieldSingularValueAccessor singularValueAccessor) {
            singularValueAccessor.setValue(entity, value, bindingResult);
        } else {
            LOG.error("Can't resolve value accessor type for field {} of model {}", field.name(), model.modelName());
        }
        validator.validate(entity, bindingResult, AdminValidation.class, Default.class);
        if (bindingResult.hasErrors()) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        }
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
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        }

        return new AdminModelEditingResult(
                getEntityDetails(model, entity, FormAction.CREATE),
                bindingResult
        );
    }
}
