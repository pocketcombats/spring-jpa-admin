package com.pocketcombats.admin.core;

import com.pocketcombats.admin.AdminValidation;
import com.pocketcombats.admin.core.field.AdminFormFieldCompositeValueAccessor;
import com.pocketcombats.admin.core.field.AdminFormFieldPluralValueAccessor;
import com.pocketcombats.admin.core.field.AdminFormFieldSingularValueAccessor;
import com.pocketcombats.admin.core.field.AdminFormFieldValueAccessor;
import com.pocketcombats.admin.core.links.AdminRelationLinkService;
import com.pocketcombats.admin.core.permission.AdminPermissionService;
import com.pocketcombats.admin.data.form.AdminFormField;
import com.pocketcombats.admin.data.form.AdminFormFieldGroup;
import com.pocketcombats.admin.data.form.AdminRelationLink;
import com.pocketcombats.admin.data.form.EntityDetails;
import com.pocketcombats.admin.history.AdminHistoryWriter;
import com.pocketcombats.admin.util.EntityUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.PersistenceUnitUtil;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.validation.groups.Default;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.convert.ConversionException;
import org.springframework.core.convert.ConversionService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.SmartValidator;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class AdminModelFormServiceImpl implements AdminModelFormService {

    private static final Logger LOG = LoggerFactory.getLogger(AdminModelFormServiceImpl.class);

    private final AdminModelRegistry modelRegistry;
    private final AdminHistoryWriter historyWriter;
    private final AdminRelationLinkService relationLinkService;
    private final EntityManager em;
    private final SmartValidator validator;
    private final AdminPermissionService permissionService;
    private final ConversionService conversionService;
    private final MessageSource messageSource;

    public AdminModelFormServiceImpl(
            AdminModelRegistry modelRegistry,
            AdminHistoryWriter historyWriter,
            AdminRelationLinkService relationLinkService,
            EntityManager em,
            SmartValidator validator,
            AdminPermissionService permissionService,
            ConversionService conversionService,
            MessageSource messageSource
    ) {
        this.modelRegistry = modelRegistry;
        this.historyWriter = historyWriter;
        this.relationLinkService = relationLinkService;
        this.em = em;
        this.validator = validator;
        this.permissionService = permissionService;
        this.conversionService = conversionService;
        this.messageSource = messageSource;
    }

    private @Nullable String resolveId(Object entity, FormAction action) {
        if (action == FormAction.CREATE && !em.contains(entity)) {
            // Not persisted (yet): the form must keep posting to the create endpoint
            return null;
        }
        return EntityUtils.getEntityStringId(em, conversionService, entity);
    }

    @Override
    @Transactional(readOnly = true)
    public EntityDetails details(String modelName, String stringId) throws UnknownModelException {
        AdminRegisteredModel model = modelRegistry.resolve(modelName);

        if (!permissionService.canView(model)) {
            throw new AccessDeniedException("You don't have permission to view " + modelName);
        }

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
                resolveId(entity, action),
                model.label(),
                formFieldGroups,
                links,
                model.actions().containsKey("delete")
        );
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Object findEntity(AdminRegisteredModel model, String stringId) throws UnknownModelException {
        RegisteredEntityDetails entityDetails = model.entityDetails();
        Object id;
        try {
            id = conversionService.convert(stringId, entityDetails.idAttribute().getJavaType());
        } catch (ConversionException e) {
            throw new UnknownModelException("No " + model.modelName() + " with id " + stringId);
        }
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<?> q = cb.createQuery(entityDetails.entityClass());
        Root<?> root = q.from(entityDetails.entityClass());
        q.where(
                cb.equal(root.get((SingularAttribute) entityDetails.idAttribute()), id)
                // TODO: support additional predicates from config
        );
        Object entity = em.createQuery(q).getSingleResultOrNull();
        if (entity == null) {
            throw new UnknownModelException("No " + model.modelName() + " with id " + stringId);
        }
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
                                        field.valueAccessor().getModelAttributes(entity)
                                ))
                                .toList()
                ))
                .toList();
    }

    /**
     * Determines if a field is editable based on the model, field, action, and user permissions.
     * <p>
     * For UPDATE actions, a field is editable if:
     * 1. The model is updatable
     * 2. The field is updatable
     * 3. The user has the "edit" permission for the model
     * <p>
     * For CREATE actions, a field is editable if it is "insertable".
     * User permission for entity creation should be checked before that.
     */
    private boolean isEditable(AdminRegisteredModel model, AdminModelField field, FormAction action) {
        return switch (action) {
            case UPDATE -> model.updatable() && field.updatable() && permissionService.canEdit(model);
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

        if (!permissionService.canEdit(model)) {
            throw new AccessDeniedException("You don't have permission to edit " + modelName);
        }

        Object entity = findEntity(model, stringId);
        BindingResult bindingResult;
        if (model.updatable()) {
            historyWriter.record(model, "edit", entity);

            // Bind straight into the managed entity, deferring every flush to the transaction
            // outcome: on binding errors the transaction rolls back and nothing is written, but the
            // re-render's queries (option lists, autocomplete probes) must not auto-flush a
            // half-bound — possibly invalid — entity in the meantime. Keeping the entity managed is
            // also what lets the re-render read lazy relations, read-only to-many fields included.
            // The persistence context is transaction-scoped, so the mode needs no restoring.
            em.setFlushMode(FlushModeType.COMMIT);
            bindingResult = bind(model, entity, FormAction.UPDATE, rawData);
            if (bindingResult.hasErrors()) {
                setRollbackOnly();
            }
        } else {
            bindingResult = new BeanPropertyBindingResult(entity, model.modelName());
        }

        return new AdminModelEditingResult(
                getEntityDetails(model, entity, FormAction.UPDATE),
                bindingResult
        );
    }

    // Overridable seam: lets tests invoke the service outside a Spring-managed transaction
    protected void setRollbackOnly() {
        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
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
            String parameterName = "model-field-" + field.name();
            AdminFormFieldValueAccessor accessor = field.valueAccessor();
            if (accessor instanceof AdminFormFieldCompositeValueAccessor compositeValueAccessor) {
                compositeValueAccessor.bind(parameterName, entity, rawData, bindingResult);
            } else if (accessor instanceof AdminFormFieldSingularValueAccessor singularValueAccessor) {
                singularValueAccessor.setValue(entity, rawData.getFirst(parameterName), bindingResult);
            } else if (accessor instanceof AdminFormFieldPluralValueAccessor pluralValueAccessor) {
                pluralValueAccessor.setValues(entity, rawData.get(parameterName), bindingResult);
            } else {
                LOG.error("Can't resolve value accessor type for field {} of model {}", field.name(), model.modelName());
            }
        }

        checkUniqueness(model, entity, action, bindingResult);
        validator.validate(entity, bindingResult, AdminValidation.class, Default.class);
        return bindingResult;
    }

    private void checkUniqueness(AdminRegisteredModel model, Object entity, FormAction action, BindingResult bindingResult) {
        if (model.uniqueConstraints().isEmpty()) {
            return;
        }
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<?> query = cb.createQuery(model.entityDetails().entityClass());
        Root<?> root = query.from(model.entityDetails().entityClass());
        Predicate[] predicates = model.uniqueConstraints().stream()
                .map(constraint -> constraint.createPredicate(cb, root, entity))
                .toArray(Predicate[]::new);
        query.where(cb.or(predicates));
        // COMMIT flush mode: the edited entity is managed and possibly dirty (updates bind into
        // the managed instance); auto-flush would write the candidate value and make the entity
        // conflict with itself
        List<?> resultList = em.createQuery(query)
                .setFlushMode(FlushModeType.COMMIT)
                .setMaxResults(1)
                .getResultList();
        if (!resultList.isEmpty()) {
            var existingEntity = resultList.get(0);
            if (action == FormAction.CREATE || !isSameEntity(entity, existingEntity)) {
                rejectWithConstraintViolation(model, entity, existingEntity, bindingResult);
            }
        }
    }

    // The edited instance may be transient (create) while existingEntity is managed, so "same row"
    // must be decided by identifier, not by instance equality
    private boolean isSameEntity(Object entity, Object existingEntity) {
        PersistenceUnitUtil persistenceUnitUtil = em.getEntityManagerFactory().getPersistenceUnitUtil();
        return Objects.equals(
                persistenceUnitUtil.getIdentifier(entity),
                persistenceUnitUtil.getIdentifier(existingEntity)
        );
    }

    private void rejectWithConstraintViolation(
            AdminRegisteredModel model,
            Object entity,
            Object conflictingEntity,
            BindingResult bindingResult
    ) {
        String conflictingEntityId = EntityUtils.getEntityStringId(em, conversionService, conflictingEntity);
        for (var constraint : model.uniqueConstraints()) {
            if (constraint.matches(entity, conflictingEntity)) {
                String violatingFields = constraint.getFieldLabels().stream()
                        .map(label -> {
                            String resolved = messageSource.getMessage(label, null, label, LocaleContextHolder.getLocale());
                            return resolved != null ? resolved : label;
                        })
                        .collect(Collectors.joining(", "));
                addConflictError(
                        bindingResult,
                        "spring-jpa-admin.validation.uniqueness-violation.fields.message",
                        new String[]{model.modelName(), conflictingEntityId, violatingFields},
                        model,
                        conflictingEntityId
                );
                return;
            }
        }
        LOG.warn("Could not detect constraint violation for {}", model.modelName());
        addConflictError(
                bindingResult,
                "spring-jpa-admin.validation.uniqueness-violation.message",
                new String[]{model.modelName(), conflictingEntityId},
                model,
                conflictingEntityId
        );
    }

    // Equivalent to BindingResult.reject, but the error additionally carries the conflicting
    // entity's coordinates so the form can render a "view conflicting entity" link next to the
    // plain-text message.
    private void addConflictError(
            BindingResult bindingResult,
            String code,
            String[] args,
            AdminRegisteredModel model,
            String conflictingEntityId
    ) {
        bindingResult.addError(new UniquenessViolationError(
                bindingResult.getObjectName(),
                bindingResult.resolveMessageCodes(code),
                args,
                model.modelName(),
                conflictingEntityId
        ));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public BindingResult updateField(
            String modelName,
            String stringId,
            String fieldName,
            @Nullable String value
    ) throws UnknownModelException {
        AdminRegisteredModel model = modelRegistry.resolve(modelName);

        if (!permissionService.canEdit(model)) {
            throw new AccessDeniedException("You don't have permission to edit " + modelName);
        }

        AdminModelField field = model.findFormField(fieldName)
                .orElseThrow(() -> new UnknownModelException(
                        "Model " + modelName + " has no field " + fieldName
                ));
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
        checkUniqueness(model, entity, FormAction.UPDATE, bindingResult);
        validator.validate(entity, bindingResult, AdminValidation.class, Default.class);
        if (bindingResult.hasErrors()) {
            setRollbackOnly();
        }
        return bindingResult;
    }

    @Override
    @Transactional(readOnly = true)
    public EntityDetails create(String modelName) throws UnknownModelException {
        AdminRegisteredModel model = modelRegistry.resolve(modelName);

        if (!permissionService.canCreate(model)) {
            throw new AccessDeniedException("You don't have permission to create " + modelName);
        }

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

        if (!permissionService.canCreate(model)) {
            throw new AccessDeniedException("You don't have permission to create " + modelName);
        }

        Object entity = BeanUtils.instantiateClass(model.entityDetails().entityClass());

        BindingResult bindingResult = bind(model, entity, FormAction.CREATE, rawData);
        if (!bindingResult.hasErrors()) {
            em.persist(entity);
            historyWriter.record(model, "create", entity);
        } else {
            LOG.debug("Binding result has errors, can't save new {}", model.modelName());
            setRollbackOnly();
        }

        return new AdminModelEditingResult(
                getEntityDetails(model, entity, FormAction.CREATE),
                bindingResult
        );
    }
}
