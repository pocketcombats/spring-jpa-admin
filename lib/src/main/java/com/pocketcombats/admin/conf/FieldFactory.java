package com.pocketcombats.admin.conf;

import com.pocketcombats.admin.AdminField;
import com.pocketcombats.admin.AdminFieldOverride;
import com.pocketcombats.admin.AdminModel;
import com.pocketcombats.admin.core.AdminModelField;
import com.pocketcombats.admin.core.AdminModelListField;
import com.pocketcombats.admin.core.field.AdminFormFieldValueAccessor;
import com.pocketcombats.admin.core.field.BooleanFormFieldValueAccessor;
import com.pocketcombats.admin.core.field.DelegatingAdminFormFieldValueAccessorImpl;
import com.pocketcombats.admin.core.field.EnumFormFieldValueAccessor;
import com.pocketcombats.admin.core.field.RawIdFormFieldAccessor;
import com.pocketcombats.admin.core.field.ToManyFormFieldAccessor;
import com.pocketcombats.admin.core.field.ToOneFormFieldAccessor;
import com.pocketcombats.admin.core.filter.AdminModelFilter;
import com.pocketcombats.admin.core.filter.BasicFilterOptionsCollector;
import com.pocketcombats.admin.core.filter.BooleanFilterOptionsCollector;
import com.pocketcombats.admin.core.filter.FilterOptionsCollector;
import com.pocketcombats.admin.core.filter.ToManyFilterOptionsCollector;
import com.pocketcombats.admin.core.filter.ToOneFilterOptionsCollector;
import com.pocketcombats.admin.core.formatter.SpelExpressionContextFactory;
import com.pocketcombats.admin.core.formatter.SpelExpressionFormatter;
import com.pocketcombats.admin.core.formatter.ToStringValueFormatter;
import com.pocketcombats.admin.core.formatter.ValueFormatter;
import com.pocketcombats.admin.core.predicate.BasicPredicateFactory;
import com.pocketcombats.admin.core.predicate.ToManyPredicateFactory;
import com.pocketcombats.admin.core.predicate.ToOnePredicateFactory;
import com.pocketcombats.admin.core.predicate.ValuePredicateFactory;
import com.pocketcombats.admin.core.property.*;
import com.pocketcombats.admin.core.sort.PathSortExpressionFactory;
import com.pocketcombats.admin.core.sort.SimpleSortExpressionFactory;
import com.pocketcombats.admin.core.sort.SortExpressionFactory;
import com.pocketcombats.admin.core.uniqueness.SingleAdminUniqueConstraint;
import com.pocketcombats.admin.util.AdminStringUtils;
import com.pocketcombats.admin.util.TypeUtils;
import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.EntityManager;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.util.StringUtils;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class FieldFactory {

    private static final Logger LOG = LoggerFactory.getLogger(FieldFactory.class);

    private final EntityManager em;
    private final ConversionService conversionService;
    private final SpelExpressionContextFactory spelExpressionContextFactory;

    private final String modelName;
    private final EntityType<?> entity;
    private final Class<?> targetClass;
    private final @Nullable Class<?> modelAdminClass;
    private final @Nullable Object adminModelBean;

    private final Map<String, AdminField> fieldOverrides;
    private final Map<String, AdminField> resolvedFieldsConfigurations = new HashMap<>();
    private final Map<String, ValueFormatter> fieldValueFormatters = new HashMap<>();

    public FieldFactory(
            EntityManager em,
            ConversionService conversionService,
            SpelExpressionContextFactory spelExpressionContextFactory,
            String modelName,
            AdminModel modelAnnotation,
            Class<?> targetClass,
            EntityType<?> entity,
            @Nullable Class<?> modelAdminClass,
            @Nullable Object adminModelBean
    ) {
        this.em = em;
        this.conversionService = conversionService;
        this.spelExpressionContextFactory = spelExpressionContextFactory;

        this.modelName = modelName;
        this.entity = entity;
        this.targetClass = targetClass;
        this.modelAdminClass = modelAdminClass;
        this.adminModelBean = adminModelBean;

        this.fieldOverrides = Arrays.stream(modelAnnotation.fieldOverrides())
                .collect(Collectors.toMap(
                        AdminFieldOverride::name,
                        AdminFieldOverride::field
                ));
    }

    public AdminModelListField constructListField(String name) {
        String label = null;
        String emptyValue = "—";

        AdminField fieldConfig = resolveFieldConfig(name);
        if (fieldConfig != null) {
            label = fieldConfig.label();
            emptyValue = fieldConfig.emptyValue();
        }

        if (!StringUtils.hasText(label)) {
            label = AdminStringUtils.toHumanReadableName(name);
        }

        AdminModelPropertyReader reader = resolveListFieldReader(name);
        ValueFormatter formatter;
        if (TypeUtils.isBasicType(reader.getJavaType())) {
            formatter = null;
        } else {
            formatter = createValueFormatter(name);
        }

        SortExpressionFactory sortExpressionFactory = null;
        if (fieldConfig != null) {
            if (!fieldConfig.sortBy().isEmpty()) {
                sortExpressionFactory = new PathSortExpressionFactory(name + "." + fieldConfig.sortBy());
            } else if (fieldConfig.sortable()) {
                sortExpressionFactory = new SimpleSortExpressionFactory(name);
            }
        }

        return new AdminModelListField(
                name, label, emptyValue,
                reader, formatter,
                sortExpressionFactory
        );
    }

    public AdminModelField constructFormField(String name) {
        String label = null;
        String description = null;
        String template = null;
        boolean insertable = true;
        boolean updatable = true;

        AdminField fieldConfig = resolveFieldConfig(name);
        if (fieldConfig != null) {
            label = fieldConfig.label();
            description = fieldConfig.description();
            template = fieldConfig.template();
            insertable = fieldConfig.insertable();
            updatable = fieldConfig.updatable();
        }
        Annotation columnAnnotation = resolveColumnAnnotation(name);
        if (columnAnnotation != null) {
            Map<String, Object> annotationAttributes = AnnotationUtils.getAnnotationAttributes(columnAnnotation);
            insertable = insertable && (Boolean) annotationAttributes.get("insertable");
            updatable = updatable && (Boolean) annotationAttributes.get("updatable");
        }

        if (!StringUtils.hasText(label)) {
            label = AdminStringUtils.toHumanReadableName(name);
        }
        if (!StringUtils.hasText(description)) {
            description = null;
        }

        AdminFormFieldValueAccessor formFieldAccessor = resolveFormFieldAccessor(fieldConfig, name);

        if (!StringUtils.hasText(template)) {
            template = formFieldAccessor.getDefaultTemplate();
        }

        return new AdminModelField(
                name,
                label,
                description,
                template,
                insertable,
                updatable,
                formFieldAccessor
        );
    }

    public AdminModelFilter constructFieldFilter(String name) {
        return new AdminModelFilter(
                name,
                resolveFieldLabel(name),
                constructValuePredicateFactory(name),
                constructFilterOptionsCollector(name)
        );
    }

    public SingleAdminUniqueConstraint constructFieldConstraint(String name) {
        return new SingleAdminUniqueConstraint(
                resolveFieldLabel(name),
                constructValuePredicateFactory(name),
                resolveFormFieldReader(name)
        );
    }

    private String resolveFieldLabel(String name) {
        String label = null;
        AdminField fieldConfig = resolveFieldConfig(name);
        if (fieldConfig != null) {
            label = fieldConfig.label();
        }
        if (!StringUtils.hasText(label)) {
            label = AdminStringUtils.toHumanReadableName(name);
        }
        return label;
    }

    private ValuePredicateFactory constructValuePredicateFactory(String name) {
        Attribute<?, ?> attribute = entity.getAttribute(name);
        Attribute.PersistentAttributeType attributeType = attribute.getPersistentAttributeType();
        return switch (attributeType) {
            case ONE_TO_ONE, MANY_TO_ONE -> new ToOnePredicateFactory(em, conversionService, attribute);
            case ONE_TO_MANY, MANY_TO_MANY -> new ToManyPredicateFactory(em, conversionService, attribute);
            case BASIC -> new BasicPredicateFactory(conversionService, attribute);
            default -> throw new IllegalArgumentException(
                    "Unsupported value predicate attribute type: " + attributeType + " (field " + name + " of model " + modelName + ")"
            );
        };
    }

    private FilterOptionsCollector constructFilterOptionsCollector(String name) {
        Attribute<?, ?> attribute = entity.getAttribute(name);
        Attribute.PersistentAttributeType attributeType = attribute.getPersistentAttributeType();
        return switch (attributeType) {
            case ONE_TO_ONE, MANY_TO_ONE -> new ToOneFilterOptionsCollector(
                    em, conversionService,
                    entity, attribute,
                    createValueFormatter(name)
            );
            case ONE_TO_MANY, MANY_TO_MANY -> new ToManyFilterOptionsCollector(
                    em, conversionService,
                    entity, attribute,
                    createValueFormatter(name)
            );
            case BASIC -> TypeUtils.isBoolean(attribute.getJavaType())
                    ? new BooleanFilterOptionsCollector(em, entity, attribute)
                    : new BasicFilterOptionsCollector(em, conversionService, entity, attribute);
            default -> throw new IllegalArgumentException(
                    "Unsupported filter attribute type: " + attributeType + " (field " + name + " of model " + modelName + ")"
            );
        };
    }

    private @Nullable AdminField resolveFieldConfig(String name) {
        if (resolvedFieldsConfigurations.containsKey(name)) {
            return resolvedFieldsConfigurations.get(name);
        }

        AdminField fieldConfig = fieldOverrides.get(name);
        if (fieldConfig != null) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Model \"{}\" field \"{}\" has overridden configuration", targetClass.getSimpleName(), name);
            }
        } else {
            fieldConfig = discoverFieldConfig(name);
        }
        if (fieldConfig == null) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Model \"{}\" field \"{}\" has no explicit configuration", targetClass.getSimpleName(), name);
            }
        }

        resolvedFieldsConfigurations.put(name, fieldConfig);
        return fieldConfig;
    }

    private @Nullable AdminField discoverFieldConfig(String name) {
        // In most cases @AdminField will be applied directly to entity field
        try {
            Attribute<?, ?> attribute = entity.getAttribute(name);
            if (attribute != null) {
                AdminField annotation = AnnotationUtils.getAnnotation(
                        (AnnotatedElement) attribute.getJavaMember(),
                        AdminField.class
                );
                if (annotation != null) {
                    return annotation;
                }
            }
        } catch (IllegalArgumentException e) {
            // No such attribute
        }

        // If not, it's likely to be declared at admin model level
        if (modelAdminClass != null) {
            Method method = findModelAdminPropertyReader(modelAdminClass, name, targetClass);
            if (method != null) {
                AdminField annotation = AnnotationUtils.getAnnotation(
                        method,
                        AdminField.class
                );
                if (annotation != null) {
                    return annotation;
                }
            }
        }

        return null;
    }

    private AdminModelPropertyReader resolveListFieldReader(String name) {
        AdminModelPropertyReader propertyAccessor = findAdminModelPropertyReader(name);
        if (propertyAccessor == null) {
            propertyAccessor = findEntityPropertyReader(name);
        }
        if (propertyAccessor == null) {
            propertyAccessor = findEntityFieldReader(name);
        }
        if (propertyAccessor == null) {
            // Can we hint available properties?
            throw new IllegalArgumentException("Can't find list field reader for " + entity.getName() + "." + name);
        }
        return propertyAccessor;
    }

    private @Nullable AdminModelPropertyReader findAdminModelPropertyReader(String name) {
        if (modelAdminClass == null) {
            return null;
        }
        Method method = findModelAdminPropertyReader(modelAdminClass, name, targetClass);
        if (method != null) {
            return new AdminModelDelegatingPropertyReader(name, adminModelBean, method);
        }
        return null;
    }

    private @Nullable AdminModelPropertyWriter findAdminModelPropertyWriter(String name) {
        Method method = findModelAdminPropertyWriter(modelAdminClass, name, targetClass);
        if (method != null) {
            return new AdminModelDelegatingPropertyWriter(name, adminModelBean, method);
        }
        return null;
    }

    private @Nullable AdminModelPropertyReader findEntityPropertyReader(String name) {
        PropertyDescriptor pd = BeanUtils.getPropertyDescriptor(targetClass, name);
        if (pd != null) {
            return new MethodPropertyReader(pd.getName(), pd.getReadMethod());
        }
        return null;
    }

    private @Nullable AdminModelPropertyWriter findEntityPropertyWriter(String name) {
        PropertyDescriptor pd = BeanUtils.getPropertyDescriptor(targetClass, name);
        if (pd != null && pd.getWriteMethod() != null) {
            return new MethodPropertyWriter(pd.getName(), pd.getWriteMethod());
        }
        return null;
    }

    private @Nullable AdminModelPropertyReader findEntityFieldReader(String name) {
        Attribute<?, ?> attribute;
        try {
            attribute = entity.getAttribute(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
        Member member = attribute.getJavaMember();
        if (member instanceof Method methodMember) {
            if (methodMember.getParameterCount() != 0) {
                LOG.debug("Found method for Entity attribute {}, but can't use it", name);
            } else {
                return new MethodPropertyReader(name, methodMember);
            }
        } else if (member instanceof Field fieldMember) {
            return new FieldPropertyReader(fieldMember);
        }

        LOG.debug("Found Entity attribute {}, but can't use it; expected either Method or Field", name);
        return null;
    }

    private @Nullable AdminModelPropertyWriter findEntityFieldWriter(String name) {
        Attribute<?, ?> attribute;
        try {
            attribute = entity.getAttribute(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
        Member member = attribute.getJavaMember();
        if (member instanceof Field fieldMember) {
            return new FieldPropertyWriter(fieldMember);
        }
        LOG.debug("Found Entity attribute {}, but can't use it; expected Field", name);
        return null;
    }

    private @Nullable Method findModelAdminPropertyReader(Class<?> modelAdminClass, String name, Class<?> targetClass) {
        Method method = tryFindModelAdminNamedPropertyReader(modelAdminClass, StringUtils.capitalize(name), targetClass);
        if (method == null) {
            method = tryFindModelAdminNamedPropertyReader(modelAdminClass, name, targetClass);
        }
        if (method == null) {
            method = tryFindModelAdminNamedPropertyReader(modelAdminClass, StringUtils.uncapitalize(name), targetClass);
        }
        return method;
    }

    private @Nullable Method tryFindModelAdminNamedPropertyReader(
            Class<?> modelAdminClass,
            String name,
            Class<?> targetClass
    ) {
        Method method = BeanUtils.findMethod(modelAdminClass, "get" + name, targetClass);
        if (method == null) {
            method = BeanUtils.findMethod(modelAdminClass, "is" + name, targetClass);
        }
        if (method == null) {
            method = BeanUtils.findMethod(modelAdminClass, name, targetClass);
        }
        return method;
    }

    private @Nullable Method findModelAdminPropertyWriter(Class<?> modelAdminClass, String name, Class<?> targetClass) {
        Method method = tryFindModelAdminNamedPropertyWriter(modelAdminClass, StringUtils.capitalize(name), targetClass);
        if (method == null) {
            method = tryFindModelAdminNamedPropertyWriter(modelAdminClass, name, targetClass);
        }
        if (method == null) {
            method = tryFindModelAdminNamedPropertyWriter(modelAdminClass, StringUtils.uncapitalize(name), targetClass);
        }
        return method;
    }

    private @Nullable Method tryFindModelAdminNamedPropertyWriter(
            Class<?> modelAdminClass,
            String name,
            Class<?> targetClass
    ) {
        Method method = BeanUtils.findMethod(modelAdminClass, "set" + name, targetClass);
        if (method == null) {
            method = BeanUtils.findMethod(modelAdminClass, name, targetClass);
        }
        return method;
    }

    private AdminFormFieldValueAccessor resolveFormFieldAccessor(@Nullable AdminField fieldConfig, String name) {
        // Contrary to list field readers, form field readers and writers are intended to try entity attributes first
        // We could opt to work with Attribute#javaMember directly, but this would break e.g. hibernate enhancer
        Attribute<?, ?> attribute = null;
        try {
            attribute = entity.getAttribute(name);
        } catch (IllegalArgumentException e) {
            LOG.trace("Can't find attribute for model {} field \"{}\"", modelName, name);
        }

        AdminModelPropertyReader reader = resolveFormFieldReader(name);
        // Custom form fields are only supported for "simple" types.
        if (attribute == null && !isCustomFormFieldSupportedType(reader.getJavaType())) {
            throw new IllegalStateException(
                    "Unsupported type for custom model admin form field: " + reader.getJavaType().getName() +
                            " (field " + name + " of model " + modelName + ")"
            );
        } else {
            // TODO: validate reader and attribute types matching?
        }

        AdminModelPropertyWriter writer = resolveFormFieldWriter(name);

        return constructFormFieldValueAccessor(name, fieldConfig, attribute, reader, writer);
    }

    private AdminModelPropertyReader resolveFormFieldReader(String name) {
        AdminModelPropertyReader reader = findEntityPropertyReader(name);
        if (reader == null) {
            reader = findEntityFieldReader(name);
        }
        if (reader == null) {
            reader = findAdminModelPropertyReader(name);
        }
        if (reader == null) {
            // Can we hint available properties?
            throw new IllegalArgumentException("Can't find form field reader for " + entity.getName() + "." + name);
        }
        return reader;
    }

    private @Nullable AdminModelPropertyWriter resolveFormFieldWriter(String name) {
        AdminModelPropertyWriter writer = findEntityPropertyWriter(name);
        if (writer == null) {
            writer = findEntityFieldWriter(name);
        }
        if (writer == null) {
            writer = findAdminModelPropertyWriter(name);
        }
        return writer;
    }

    private boolean isCustomFormFieldSupportedType(Class<?> type) {
        return TypeUtils.isBasicType(type);
    }

    private AdminFormFieldValueAccessor constructFormFieldValueAccessor(
            String name,
            @Nullable AdminField fieldConfig,
            @Nullable Attribute<?, ?> attribute,
            AdminModelPropertyReader reader,
            @Nullable AdminModelPropertyWriter writer
    ) {
        if (attribute != null) {
            Attribute.PersistentAttributeType persistentAttributeType = attribute.getPersistentAttributeType();
            return switch (persistentAttributeType) {
                case MANY_TO_ONE, ONE_TO_ONE -> createToOneFormFieldAccessor(
                        name,
                        fieldConfig,
                        attribute, reader, writer
                );
                case MANY_TO_MANY, ONE_TO_MANY -> new ToManyFormFieldAccessor(
                        em, conversionService,
                        attribute, reader, writer, createValueFormatter(name)
                );
                case BASIC -> selectBasicFormFieldAccessor(name, isOptional(fieldConfig, attribute), reader, writer);
                default -> throw new IllegalStateException(
                        "Unsupported attribute type: " + persistentAttributeType +
                                " (field " + name + " of model " + modelName + ")"
                );
            };
        }
        boolean optional;
        if (fieldConfig != null) {
            optional = fieldConfig.nullable();
        } else {
            optional = true;
        }
        return selectBasicFormFieldAccessor(name, optional, reader, writer);
    }

    private AdminFormFieldValueAccessor createToOneFormFieldAccessor(
            String name,
            @Nullable AdminField fieldConfig,
            Attribute<?, ?> attribute,
            AdminModelPropertyReader reader,
            @Nullable AdminModelPropertyWriter writer
    ) {
        if (isRawId(fieldConfig)) {
            return new RawIdFormFieldAccessor(
                    em, conversionService,
                    attribute,
                    isOptional(fieldConfig, attribute),
                    reader, writer
            );
        } else {
            return new ToOneFormFieldAccessor(
                    em, conversionService,
                    attribute,
                    isOptional(fieldConfig, attribute),
                    reader, writer, createValueFormatter(name)
            );
        }
    }

    private ValueFormatter createValueFormatter(String fieldName) {
        if (!fieldValueFormatters.containsKey(fieldName)) {
            AdminField fieldConfiguration = resolveFieldConfig(fieldName);
            if (fieldConfiguration == null) {
                fieldValueFormatters.put(fieldName, new ToStringValueFormatter());
            } else {
                ValueFormatter formatter;
                if (fieldConfiguration.representation().equals("")) {
                    formatter = new ToStringValueFormatter();
                } else {
                    formatter = new SpelExpressionFormatter(
                            spelExpressionContextFactory,
                            fieldConfiguration.representation()
                    );
                }
                fieldValueFormatters.put(
                        fieldName,
                        formatter
                );
            }
        }
        return fieldValueFormatters.get(fieldName);
    }

    private static boolean isRawId(@Nullable AdminField fieldConfig) {
        if (fieldConfig == null) {
            return false;
        }
        return fieldConfig.rawId();
    }

    private static boolean isOptional(@Nullable AdminField fieldConfig, Attribute<?, ?> attribute) {
        if (fieldConfig != null && !fieldConfig.nullable()) {
            return false;
        }
        return ((SingularAttribute<?, ?>) attribute).isOptional();
    }

    @SuppressWarnings("unchecked")
    private AdminFormFieldValueAccessor selectBasicFormFieldAccessor(
            String name,
            boolean optional,
            AdminModelPropertyReader reader,
            AdminModelPropertyWriter writer
    ) {
        Class<?> type = reader.getJavaType();
        if (TypeUtils.isBoolean(type)) {
            return new BooleanFormFieldValueAccessor(name, reader, writer);
        } else if (Enum.class.isAssignableFrom(type)) {
            return new EnumFormFieldValueAccessor(
                    name,
                    (Class<? extends Enum<?>>) type,
                    optional,
                    reader, writer,
                    createValueFormatter(name)
            );
        }
        return new DelegatingAdminFormFieldValueAccessorImpl(name, conversionService, reader, writer);
    }

    private @Nullable Annotation resolveColumnAnnotation(String fieldName) {
        Attribute<?, ?> attribute;
        try {
            attribute = entity.getAttribute(fieldName);
        } catch (IllegalArgumentException e) {
            return null;
        }
        return resolveColumnAnnotation(attribute);
    }

    public static @Nullable Annotation resolveColumnAnnotation(Attribute<?, ?> attribute) {
        AnnotatedElement javaMember = (AnnotatedElement) attribute.getJavaMember();
        Column columnAnnotation = AnnotationUtils.getAnnotation(javaMember, Column.class);
        if (columnAnnotation != null) {
            return columnAnnotation;
        }
        return AnnotationUtils.getAnnotation(javaMember, JoinColumn.class);
    }
}
