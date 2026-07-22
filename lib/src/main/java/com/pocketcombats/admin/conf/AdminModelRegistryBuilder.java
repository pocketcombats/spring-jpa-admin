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
import com.pocketcombats.admin.core.search.SearchPredicateFactory;
import com.pocketcombats.admin.core.search.SearchPredicateFactoryResolver;
import com.pocketcombats.admin.core.uniqueness.AdminUniqueConstraint;
import com.pocketcombats.admin.core.uniqueness.CompositeAdminUniqueConstraint;
import com.pocketcombats.admin.util.AdminStringUtils;
import com.pocketcombats.admin.util.PackageAnnotationFinder;
import jakarta.annotation.Priority;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.ConversionService;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/* package */ class AdminModelRegistryBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(AdminModelRegistryBuilder.class);

    private final Map<String, AdminModelDescriptor> descriptors = new HashMap<>();

    private final EntityManager em;
    private final AutowireCapableBeanFactory beanFactory;
    private final ConversionService conversionService;
    private final SpelExpressionContextFactory spelExpressionContextFactory;
    private final AdminModelLinkFactory linksFactory;
    private final ActionsFactory actionsFactory;
    private final SearchPredicateFactoryResolver searchPredicateFactoryResolver;
    private final int maxPreloadedOptions;
    private final int maxCountedOptions;

    public AdminModelRegistryBuilder(
            EntityManager em,
            AutowireCapableBeanFactory beanFactory,
            ConversionService conversionService,
            SpelExpressionContextFactory spelExpressionContextFactory,
            AdminModelLinkFactory linksFactory,
            ActionsFactory actionsFactory,
            int maxPreloadedOptions,
            int maxCountedOptions
    ) {
        this.em = em;
        this.beanFactory = beanFactory;
        this.conversionService = conversionService;
        this.spelExpressionContextFactory = spelExpressionContextFactory;
        this.linksFactory = linksFactory;
        this.actionsFactory = actionsFactory;
        this.searchPredicateFactoryResolver = new SearchPredicateFactoryResolver(conversionService);
        this.maxPreloadedOptions = maxPreloadedOptions;
        this.maxCountedOptions = maxCountedOptions;
    }

    /**
     * Registers a class's metadata: name, target entity, and search configuration.
     * Construction is deferred to {@link #build()}.
     */
    public AdminModelRegistryBuilder addModel(Class<?> annotatedClass) {
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
        if (modelAnnotation.pageSize() <= 0) {
            throw new AdminConfigurationException(
                    "Admin model " + modelName + " declares invalid pageSize "
                            + modelAnnotation.pageSize() + "; pageSize must be positive"
            );
        }
        AdminModelDescriptor existing = descriptors.get(modelName);
        if (existing != null) {
            throw new IllegalArgumentException(
                    "Admin model name \"" + modelName + "\" is already registered for "
                            + existing.targetClass().getName()
                            + " and can't be reused for " + targetClass.getName()
                            + ". Model names default to the entity's simple class name;"
                            + " set @AdminModel(name = ...) to disambiguate."
            );
        }

        EntityType<?> entity = em.getEntityManagerFactory().getMetamodel().entity(targetClass);
        SearchPredicateFactory searchPredicateFactory = searchPredicateFactoryResolver.resolve(
                modelName, entity, modelAnnotation.searchFields()
        );
        PackageInfo packageInfo = resolvePackageInfo(annotatedClass, targetClass);

        descriptors.put(modelName, new AdminModelDescriptor(
                modelName, modelAnnotation, annotatedClass, targetClass, entity, searchPredicateFactory, packageInfo
        ));
        return this;
    }

    private String resolveModelName(AdminModel modelAnnotation, Class<?> targetClass) {
        if (!modelAnnotation.name().isEmpty()) {
            return modelAnnotation.name();
        }
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

    private @Nullable PackageInfo resolvePackageInfo(Class<?> aClass) {
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
     * Constructs an {@link AdminRegisteredModel} from a previously-collected {@link AdminModelDescriptor}.
     * <p>
     * This method builds all the necessary components, such as fields, actions, filters, and
     * permissions. It also resolves entity details and collects unique constraints.
     */
    private AdminRegisteredModel constructAdminModel(
            AdminModelDescriptor descriptor,
            Map<Class<?>, SearchPredicateFactory> searchFactoriesByEntity
    ) {
        long timeStart = System.nanoTime();
        String modelName = descriptor.modelName();
        AdminModel modelAnnotation = descriptor.modelAnnotation();
        Class<?> annotatedClass = descriptor.annotatedClass();
        Class<?> targetClass = descriptor.targetClass();
        EntityType<?> entity = descriptor.entity();

        Class<?> adminModelClass = annotatedClass != targetClass ? annotatedClass : null;
        AdminModelBean adminModelBean = adminModelClass != null
                ? new AdminModelBean(adminModelClass, beanFactory.createBean(adminModelClass))
                : null;
        String label = "".equals(modelAnnotation.label())
                ? AdminStringUtils.toHumanReadableName(modelName)
                : modelAnnotation.label();

        FieldFactory fieldFactory = new FieldFactory(
                em, conversionService, spelExpressionContextFactory,
                maxPreloadedOptions, maxCountedOptions, searchFactoriesByEntity,
                modelName, modelAnnotation, targetClass, entity, adminModelBean
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
                modelName, modelAnnotation, targetClass, adminModelBean
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
        AdminRegisteredModel model = new AdminRegisteredModel(
                modelName,
                priority,
                label,
                entityDetails,
                modelAnnotation.insertable(),
                modelAnnotation.updatable(),
                modelAnnotation.pageSize(),
                listFields,
                modelAnnotation.defaultOrder(),
                descriptor.searchPredicateFactory(),
                filters,
                fieldsets,
                links,
                actions,
                uniqueConstraints,
                modelAnnotation.permissions()
        );
        if (LOG.isTraceEnabled()) {
            LOG.trace(
                    "Spent {} nanoseconds constructing {} AdminRegisteredModel",
                    System.nanoTime() - timeStart,
                    modelName
            );
        }
        return model;
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

    private List<String> resolveListFieldNames(
            String modelName,
            AdminModel modelAnnotation,
            Class<?> targetClass,
            EntityType<?> entity,
            @Nullable Class<?> adminModelClass
    ) {
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
            @Nullable AdminModelBean adminModelBean
    ) {
        Map<String, AdminModelAction> actions = actionsFactory.createActions(
                modelAnnotation,
                targetClass,
                adminModelBean
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
                    Map<String, ? extends @Nullable Object> annotationAttributes
                            = AnnotationUtils.getAnnotationAttributes(columnAnnotation);
                    return Boolean.TRUE.equals(annotationAttributes.get("unique"));
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
        Map<Class<?>, SearchPredicateFactory> searchFactoriesByEntity = resolvePrimarySearchFactories();
        // Sorted by PackageInfo (priority, then package name), then grouped by category label:
        // distinct packages sharing a label (or none) merge into one entry, in that sorted order.
        Map<String, List<AdminRegisteredModel>> models = descriptors.values().stream()
                .sorted(Comparator.comparing(AdminModelDescriptor::packageInfo)
                        // Deterministic, meaningful order within a category (same PackageInfo): higher
                        // model priority first, then model name.
                        .thenComparing(Comparator.comparingInt(
                                (AdminModelDescriptor d) -> getPriority(d.annotatedClass(), d.targetClass())).reversed())
                        .thenComparing(AdminModelDescriptor::modelName))
                .collect(Collectors.groupingBy(
                        descriptor -> descriptor.packageInfo().label(),
                        LinkedHashMap::new,
                        Collectors.mapping(
                                descriptor -> constructAdminModel(descriptor, searchFactoriesByEntity),
                                Collectors.toList())
                ));
        return new AdminModelRegistryImpl(models);
    }

    /**
     * One search factory per entity class, taken from its primary searchable registration.
     */
    private Map<Class<?>, SearchPredicateFactory> resolvePrimarySearchFactories() {
        Map<Class<?>, SearchPredicateFactory> factories = new HashMap<>();
        descriptors.values().stream()
                .sorted(Comparator.comparing((AdminModelDescriptor d) ->
                                !d.modelName().equals(d.targetClass().getSimpleName()))
                        .thenComparing(AdminModelDescriptor::modelName))
                .forEach(descriptor -> {
                    SearchPredicateFactory factory = descriptor.searchPredicateFactory();
                    if (factory != null) {
                        factories.putIfAbsent(descriptor.targetClass(), factory);
                    }
                });
        return factories;
    }

    private record AdminModelDescriptor(
            String modelName,
            AdminModel modelAnnotation,
            Class<?> annotatedClass,
            Class<?> targetClass,
            EntityType<?> entity,
            @Nullable SearchPredicateFactory searchPredicateFactory,
            PackageInfo packageInfo
    ) {
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

        @Override
        public int compareTo(PackageInfo o) {
            if (o.priority != priority) {
                return Integer.compare(o.priority, priority);
            }
            return packageName.compareTo(o.packageName);
        }

        public String label() {
            return packageConfig == null ? "" : packageConfig.label();
        }
    }
}
