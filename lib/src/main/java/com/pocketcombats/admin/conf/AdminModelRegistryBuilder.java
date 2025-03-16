package com.pocketcombats.admin.conf;

import com.pocketcombats.admin.AdminModel;
import com.pocketcombats.admin.AdminPackage;
import com.pocketcombats.admin.core.AdminModelFieldset;
import com.pocketcombats.admin.core.AdminModelListField;
import com.pocketcombats.admin.core.AdminModelRegistry;
import com.pocketcombats.admin.core.AdminModelRegistryImpl;
import com.pocketcombats.admin.core.AdminRegisteredModel;
import com.pocketcombats.admin.core.RegisteredEntityDetails;
import com.pocketcombats.admin.core.action.AdminModelAction;
import com.pocketcombats.admin.core.filter.AdminModelFilter;
import com.pocketcombats.admin.core.formatter.SpelExpressionContextFactory;
import com.pocketcombats.admin.core.links.AdminModelLink;
import com.pocketcombats.admin.core.links.AdminModelLinkFactory;
import com.pocketcombats.admin.core.search.CompositeSearchPredicateFactory;
import com.pocketcombats.admin.core.search.NumberSearchPredicateFactory;
import com.pocketcombats.admin.core.search.SearchPredicateFactory;
import com.pocketcombats.admin.core.search.TextSearchPredicateFactory;
import com.pocketcombats.admin.core.search.UUIDSearchPredicateFactory;
import com.pocketcombats.admin.core.uniqueness.AdminUniqueConstraint;
import com.pocketcombats.admin.core.uniqueness.CompositeAdminUniqueConstraint;
import com.pocketcombats.admin.util.AdminStringUtils;
import com.pocketcombats.admin.util.PackageAnnotationFinder;
import jakarta.annotation.Nullable;
import jakarta.annotation.Priority;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.util.MultiValueMap;
import org.springframework.util.MultiValueMapAdapter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/* package */ class AdminModelRegistryBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(AdminModelRegistryBuilder.class);

    private final Map<String, AdminRegisteredModel> models = new HashMap<>();
    private final MultiValueMap<PackageInfo, AdminRegisteredModel> categorizedModels = new MultiValueMapAdapter<>(new TreeMap<>());

    private final EntityManager em;
    private final AutowireCapableBeanFactory beanFactory;
    private final ConversionService conversionService;
    private final SpelExpressionContextFactory spelExpressionContextFactory;
    private final AdminModelLinkFactory linksFactory;
    private final ActionsFactory actionsFactory;

    public AdminModelRegistryBuilder(
            EntityManager em,
            AutowireCapableBeanFactory beanFactory,
            ConversionService conversionService,
            SpelExpressionContextFactory spelExpressionContextFactory,
            AdminModelLinkFactory linksFactory,
            ActionsFactory actionsFactory
    ) {
        this.em = em;
        this.beanFactory = beanFactory;
        this.conversionService = conversionService;
        this.spelExpressionContextFactory = spelExpressionContextFactory;
        this.linksFactory = linksFactory;
        this.actionsFactory = actionsFactory;
    }

    public AdminModelRegistryBuilder addModel(Class<?> annotatedClass) {
        long timeStart = System.nanoTime();

        AdminModel modelAnnotation = AnnotationUtils.getAnnotation(annotatedClass, AdminModel.class);
        assert modelAnnotation != null;
        Class<?> targetClass;
        if (modelAnnotation.entity() != Void.class) {
            targetClass = modelAnnotation.entity();
        } else {
            targetClass = annotatedClass;
        }
        if (AnnotationUtils.getAnnotation(targetClass, Entity.class) == null) {
            throw new IllegalArgumentException("Class " + targetClass.getName() + " is not an Entity");
        }

        String modelName = resolveModelName(modelAnnotation, targetClass);
        if (models.containsKey(modelName)) {
            throw new IllegalArgumentException(
                    "AdminModel " + modelName + " is already registered"
            );
        }
        AdminRegisteredModel model = constructAdminModel(modelName, modelAnnotation, annotatedClass, targetClass);
        models.put(modelName, model);
        PackageInfo packageInfo = resolvePackageInfo(annotatedClass, targetClass);
        categorizedModels.add(packageInfo, model);

        if (LOG.isTraceEnabled()) {
            LOG.trace(
                    "Spent {} nanoseconds constructing {} AdminRegisteredModel",
                    System.nanoTime() - timeStart,
                    modelName
            );
        }
        return this;
    }

    private String resolveModelName(AdminModel modelAnnotation, Class<?> targetClass) {
        return targetClass.getSimpleName();
    }

    private PackageInfo resolvePackageInfo(Class<?> annotatedClass, Class<?> targetClass) {
        PackageInfo packageInfo = resolvePackageInfo(annotatedClass);
        if (packageInfo == null && annotatedClass != targetClass) {
            packageInfo = resolvePackageInfo(targetClass);
        }
        if (packageInfo == null) {
            packageInfo = PackageInfo.DEFAULT;
        }
        return packageInfo;
    }

    @Nullable
    private PackageInfo resolvePackageInfo(Class<?> aClass) {
        String packageName = aClass.getPackageName();
        ClassLoader classLoader = aClass.getClassLoader();
        Package annotatedPackage = PackageAnnotationFinder.findAnnotatedPackage(classLoader, AdminPackage.class, packageName);
        if (annotatedPackage == null) {
            return null;
        }
        AdminPackage adminPackageAnnotation = annotatedPackage.getAnnotation(AdminPackage.class);
        Priority priorityAnnotation = annotatedPackage.getAnnotation(Priority.class);
        int priority = priorityAnnotation == null ? 0 : priorityAnnotation.value();
        return new PackageInfo(packageName, priority, adminPackageAnnotation);
    }

    /**
     * Constructs an {@link AdminRegisteredModel} from an {@link AdminModel} annotation.
     * <p>
     * This method processes the annotation and creates a fully configured admin model
     * with all the necessary components, such as fields, actions, filters, and permissions.
     * It also resolves entity details, creates search predicates, and collects unique constraints.
     */
    private AdminRegisteredModel constructAdminModel(
            String modelName,
            AdminModel modelAnnotation,
            Class<?> annotatedClass,
            Class<?> targetClass
    ) {
        EntityType<?> entity = em.getEntityManagerFactory().getMetamodel().entity(targetClass);
        Class<?> adminModelClass = annotatedClass != targetClass ? annotatedClass : null;
        Object adminModelBean = adminModelClass != null ? beanFactory.createBean(adminModelClass) : null;
        String label = "".equals(modelAnnotation.label())
                ? AdminStringUtils.toHumanReadableName(modelName)
                : modelAnnotation.label();

        SearchPredicateFactory searchPredicateFactory = createModelSearchPredicateFactory(
                modelName,
                modelAnnotation,
                entity
        );

        FieldFactory fieldFactory = new FieldFactory(
                em, conversionService, spelExpressionContextFactory,
                modelName, modelAnnotation, targetClass, entity, adminModelClass, adminModelBean
        );
        List<AdminModelListField> listFields = createListFields(
                modelName, modelAnnotation, targetClass, entity, adminModelClass, fieldFactory
        );
        List<AdminModelFieldset> fieldsets = createFormFieldsets(
                modelName, modelAnnotation, targetClass, entity, adminModelClass, fieldFactory
        );

        List<AdminModelFilter> filters = createModelFilters(modelAnnotation, entity, fieldFactory);

        List<AdminModelLink> links = createModelLinks(modelName, modelAnnotation, targetClass);

        Map<String, AdminModelAction> actions = createActions(
                modelName, modelAnnotation, targetClass, adminModelClass, adminModelBean
        );

        List<AdminUniqueConstraint> uniqueConstraints = collectUniqueConstraints(
                modelName, entity, fieldsets, fieldFactory
        );

        RegisteredEntityDetails entityDetails = new RegisteredEntityDetails(
                targetClass,
                entity,
                entity.getId(entity.getIdType().getJavaType())
        );
        int priority = getPriority(annotatedClass, targetClass);
        return new AdminRegisteredModel(
                modelName,
                priority,
                label,
                entityDetails,
                modelAnnotation.insertable(),
                modelAnnotation.updatable(),
                modelAnnotation.pageSize(),
                listFields,
                modelAnnotation.defaultOrder(),
                searchPredicateFactory,
                filters,
                fieldsets,
                links,
                actions,
                uniqueConstraints,
                modelAnnotation.permissions()
        );
    }

    private int getPriority(Class<?> annotatedClass, Class<?> targetClass) {
        Priority priorityAnnotation = AnnotationUtils.getAnnotation(annotatedClass, Priority.class);
        if (priorityAnnotation == null) {
            priorityAnnotation = AnnotationUtils.getAnnotation(targetClass, Priority.class);
        }
        if (priorityAnnotation != null) {
            return priorityAnnotation.value();
        } else {
            return 0;
        }
    }

    private @Nullable SearchPredicateFactory createModelSearchPredicateFactory(
            String modelName,
            AdminModel modelAnnotation,
            EntityType<?> entity
    ) {
        if (modelAnnotation.searchFields().length > 0) {
            if (modelAnnotation.searchFields().length == 1) {
                return getSearchPredicateFactory(
                        modelName,
                        entity,
                        modelAnnotation.searchFields()[0]
                );
            } else {
                return new CompositeSearchPredicateFactory(
                        Arrays.stream(modelAnnotation.searchFields())
                                .map(searchField -> getSearchPredicateFactory(
                                        modelName,
                                        entity,
                                        searchField
                                ))
                                .toList()
                );
            }
        } else {
            return null;
        }
    }

    private SearchPredicateFactory getSearchPredicateFactory(
            String resolvedName,
            EntityType<?> entity,
            String searchField
    ) {
        final Attribute<?, ?> attribute;
        if (searchField.contains(".")) {
            var bits = StringUtils.split(searchField, '.');
            var sub = entity;
            for (int i = 0; i < bits.length - 1; i++) {
                sub = em.getEntityManagerFactory().getMetamodel()
                        .entity(entity.getAttribute(bits[i]).getJavaType());
            }
            attribute = sub.getAttribute(bits[bits.length - 1]);
        } else {
            attribute = entity.getAttribute(searchField);
        }
        Class<?> javaType = attribute.getJavaType();
        if (Number.class.isAssignableFrom(javaType)) {
            //noinspection unchecked
            return new NumberSearchPredicateFactory(
                    searchField,
                    (Class<? extends Number>) attribute.getJavaType(),
                    conversionService
            );
        } else if (CharSequence.class.isAssignableFrom(javaType)) {
            return new TextSearchPredicateFactory(searchField);
        } else if (UUID.class.isAssignableFrom(javaType)) {
            return new UUIDSearchPredicateFactory(searchField);
        } else {
            // Do we need to support search over boolean attributes?
            throw new IllegalStateException(
                    "Can't enable search for model " + resolvedName + ", field " + searchField +
                            ": unsupported type " + javaType
            );
        }
    }

    private List<AdminModelListField> createListFields(
            String modelName,
            AdminModel modelAnnotation,
            Class<?> targetClass,
            EntityType<?> entity,
            @Nullable Class<?> adminModelClass,
            FieldFactory fieldFactory
    ) {
        List<String> listFieldNames = resolveListFieldNames(modelName, modelAnnotation, targetClass, entity, adminModelClass);

        // noinspection UnnecessaryLocalVariable
        List<AdminModelListField> listFields = listFieldNames.stream()
                .map(fieldFactory::constructListField)
                .toList();
        return listFields;
    }

    private List<String> resolveListFieldNames(String modelName, AdminModel modelAnnotation, Class<?> targetClass, EntityType<?> entity, Class<?> adminModelClass) {
        List<String> listFieldNames;
        if (modelAnnotation.listFields().length > 0) {
            listFieldNames = Arrays.asList(modelAnnotation.listFields());
        } else {
            listFieldNames = collectDefaultListFieldNames(
                    entity,
                    adminModelClass,
                    targetClass
            );
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug(
                    "Discovered {} list fields for admin model {}: {}",
                    listFieldNames.size(), modelName, listFieldNames
            );
        }
        return listFieldNames;
    }

    private <X> List<String> collectDefaultListFieldNames(
            EntityType<X> entityType,
            @Nullable Class<?> adminModelClass,
            Class<?> entityClass
    ) {
        Set<Attribute<? super X, ?>> attributes = entityType.getAttributes();
        List<String> fields = new ArrayList<>(
                attributes.stream()
                        .filter(this::isDiscoverableListFieldAttribute)
                        .map(Attribute::getName)
                        .toList()
        );
        // TODO: collect AdminFields from adminModelClass and entityClass
        return fields;
    }

    private boolean isDiscoverableListFieldAttribute(Attribute<?, ?> attribute) {
        if (attribute instanceof SingularAttribute<?, ?> singularAttribute) {
            return !singularAttribute.isId() && !singularAttribute.isVersion();
        }
        return false;
    }

    private List<AdminModelFieldset> createFormFieldsets(
            String modelName,
            AdminModel modelAnnotation,
            Class<?> targetClass,
            EntityType<?> entity,
            @Nullable Class<?> adminModelClass,
            FieldFactory fieldFactory
    ) {
        List<FieldsetTemplate> fieldsetTemplates;
        if (modelAnnotation.fieldsets().length > 0) {
            fieldsetTemplates = Arrays.stream(modelAnnotation.fieldsets())
                    .map(adminFieldset -> new FieldsetTemplate(
                            adminFieldset.label(),
                            Arrays.asList(adminFieldset.fields()),
                            adminFieldset.unique()
                    ))
                    .toList();
        } else {
            fieldsetTemplates = Collections.singletonList(
                    new FieldsetTemplate(
                            null,
                            resolveDefaultFormFields(entity, adminModelClass, targetClass),
                            false
                    )
            );
        }
        return fieldsetTemplates.stream()
                .map(fieldsetTemplate -> toModelFieldset(fieldsetTemplate, fieldFactory))
                .toList();
    }

    private <X> List<String> resolveDefaultFormFields(
            EntityType<X> entityType,
            @Nullable Class<?> adminModelClass,
            Class<?> entityClass
    ) {
        Set<Attribute<? super X, ?>> attributes = entityType.getAttributes();
        List<String> fields = new ArrayList<>(
                attributes.stream()
                        .filter(this::isDiscoverableFormFieldAttribute)
                        .map(Attribute::getName)
                        .toList()
        );
        // TODO: collect AdminFields from adminModelClass and entityClass
        return fields;
    }

    private boolean isDiscoverableFormFieldAttribute(Attribute<?, ?> attribute) {
        if (attribute instanceof SingularAttribute<?, ?> singularAttribute) {
            return !singularAttribute.isId() && !singularAttribute.isVersion();
        }
        return true;
    }

    private AdminModelFieldset toModelFieldset(FieldsetTemplate fieldsetTemplate, FieldFactory fieldFactory) {
        return new AdminModelFieldset(
                fieldsetTemplate.label(),
                fieldsetTemplate.fields.stream()
                        .map(fieldFactory::constructFormField)
                        .toList(),
                fieldsetTemplate.unique()
        );
    }

    private List<AdminModelFilter> createModelFilters(AdminModel modelAnnotation, EntityType<?> entityType, FieldFactory fieldFactory) {
        List<AdminModelFilter> filters = new ArrayList<>(modelAnnotation.filterFields().length);

        List<AdminModelFilter> fieldFilters = Arrays.stream(modelAnnotation.filterFields())
                .map(fieldFactory::constructFieldFilter)
                .toList();
        filters.addAll(fieldFilters);
        return filters;
    }

    private List<AdminModelLink> createModelLinks(
            String modelName,
            AdminModel modelAnnotation,
            Class<?> modelType
    ) {
        return linksFactory.createModelLinks(modelName, modelAnnotation, modelType);
    }

    private Map<String, AdminModelAction> createActions(
            String modelName,
            AdminModel modelAnnotation,
            Class<?> targetClass,
            @Nullable Class<?> adminModelClass,
            @Nullable Object adminModelBean
    ) {
        Map<String, AdminModelAction> actions = actionsFactory.createActions(
                modelAnnotation,
                targetClass,
                adminModelClass, adminModelBean
        );
        if (LOG.isTraceEnabled()) {
            LOG.trace("Model {} has actions of {}", modelName, actions.keySet());
        }
        return actions;
    }

    private List<AdminUniqueConstraint> collectUniqueConstraints(
            String modelName,
            EntityType<?> entityType,
            List<AdminModelFieldset> fieldsets,
            FieldFactory fieldFactory
    ) {
        // Single attribute unique constraints
        var singleAttributeConstraints = entityType.getAttributes().stream()
                .filter(attribute -> {
                    Annotation columnAnnotation = FieldFactory.resolveColumnAnnotation(attribute);
                    if (columnAnnotation == null) {
                        return false;
                    }
                    Map<String, Object> annotationAttributes = AnnotationUtils.getAnnotationAttributes(columnAnnotation);
                    return (Boolean) annotationAttributes.get("unique");
                })
                .<AdminUniqueConstraint>map(attribute -> fieldFactory.constructFieldConstraint(attribute.getName()))
                .toList();
        // Multi-attribute unique constraints
        var compositeConstraints = fieldsets.stream()
                .filter(AdminModelFieldset::unique)
                .<AdminUniqueConstraint>map(fieldset -> {
                    var constraints = fieldset.fields().stream()
                            .map(field -> fieldFactory.constructFieldConstraint(field.name()))
                            .toList();
                    return new CompositeAdminUniqueConstraint(constraints);
                })
                .toList();

        if (!singleAttributeConstraints.isEmpty() || !compositeConstraints.isEmpty()) {
            Method entityEqualsMethod = BeanUtils.findMethod(entityType.getJavaType(), "equals", Object.class);
            if (entityEqualsMethod == null) {
                throw new IllegalStateException("Model " + modelName + " has no equals method. Is it possible?");
            }
            if (entityEqualsMethod.getDeclaringClass().equals(Object.class)) {
                throw new AdminConfigurationException(
                        modelName + " defines unique constraints but does not override equals"
                );
            }
        }

        if (compositeConstraints.isEmpty()) {
            return singleAttributeConstraints;
        } else {
            List<AdminUniqueConstraint> constraints = new ArrayList<>(singleAttributeConstraints);
            constraints.addAll(compositeConstraints);
            return constraints;
        }
    }

    public AdminModelRegistry build() {
        Map<String, List<AdminRegisteredModel>> models = categorizedModels.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> entry.getKey().label(),
                        Map.Entry::getValue,
                        (a, b) -> {
                            List<AdminRegisteredModel> result = new ArrayList<>(a);
                            result.addAll(b);
                            return result;
                        },
                        LinkedHashMap::new
                ));
        return new AdminModelRegistryImpl(models);
    }

    private record FieldsetTemplate(
            @Nullable String label,
            List<String> fields,
            boolean unique
    ) {
    }

    private record PackageInfo(
            String packageName,
            int priority,
            @Nullable AdminPackage packageConfig
    ) implements Comparable<PackageInfo> {

        private static final PackageInfo DEFAULT = new PackageInfo("", Integer.MAX_VALUE, null);

        private PackageInfo(String packageName, int priority, AdminPackage packageConfig) {
            this.packageName = packageName;
            this.priority = priority;
            this.packageConfig = packageConfig;
        }

        @Override
        public int compareTo(PackageInfo o) {
            if (o.priority != priority) {
                return o.priority - priority;
            }
            return packageName.compareTo(o.packageName);
        }

        public String label() {
            return packageConfig == null ? "" : packageConfig.label();
        }
    }
}
