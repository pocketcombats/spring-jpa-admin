package com.pocketcombats.admin.conf;

import com.pocketcombats.admin.AdminField;
import com.pocketcombats.admin.AdminFieldOverride;
import com.pocketcombats.admin.AdminModel;
import com.pocketcombats.admin.core.AdminModelField;
import com.pocketcombats.admin.core.AdminModelListField;
import com.pocketcombats.admin.core.field.AdminFormFieldValueAccessor;
import com.pocketcombats.admin.core.field.BooleanFormFieldValueAccessor;
import com.pocketcombats.admin.core.field.DelegatingAdminFormFieldValueAccessorImpl;
import com.pocketcombats.admin.core.field.ToManyFormFieldAccessor;
import com.pocketcombats.admin.core.field.ToOneFormFieldAccessor;
import com.pocketcombats.admin.core.filter.AdminModelFilter;
import com.pocketcombats.admin.core.filter.BasicFilterOptionsCollector;
import com.pocketcombats.admin.core.filter.BasicFilterPredicateFactory;
import com.pocketcombats.admin.core.filter.BooleanFilterOptionsCollector;
import com.pocketcombats.admin.core.filter.ToManyFilterOptionsCollector;
import com.pocketcombats.admin.core.filter.ToManyFilterPredicateFactory;
import com.pocketcombats.admin.core.filter.ToOneFilterOptionsCollector;
import com.pocketcombats.admin.core.filter.ToOneFilterPredicateFactory;
import com.pocketcombats.admin.core.formatter.SpelExpressionFormatter;
import com.pocketcombats.admin.core.formatter.ToStringValueFormatter;
import com.pocketcombats.admin.core.formatter.ValueFormatter;
import com.pocketcombats.admin.core.property.AdminModelDelegatingPropertyReader;
import com.pocketcombats.admin.core.property.AdminModelDelegatingPropertyWriter;
import com.pocketcombats.admin.core.property.AdminModelPropertyReader;
import com.pocketcombats.admin.core.property.AdminModelPropertyWriter;
import com.pocketcombats.admin.core.property.FieldPropertyReader;
import com.pocketcombats.admin.core.property.FieldPropertyWriter;
import com.pocketcombats.admin.core.property.MethodPropertyReader;
import com.pocketcombats.admin.core.property.MethodPropertyWriter;
import com.pocketcombats.admin.core.sort.PathSortExpressionFactory;
import com.pocketcombats.admin.core.sort.SimpleSortExpressionFactory;
import com.pocketcombats.admin.core.sort.SortExpressionFactory;
import com.pocketcombats.admin.util.AdminStringUtils;
import com.pocketcombats.admin.util.TypeUtils;
import jakarta.annotation.Nullable;
import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EntityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.util.StringUtils;

import java.beans.PropertyDescriptor;
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
    private final AutowireCapableBeanFactory beanFactory;

    private final String modelName;
    private final AdminModel modelAnnotation;
    private final EntityType<?> entity;
    @Nullable
    private final Class<?> modelAdminClass;
    @Nullable
    private final Object adminModelBean;
    private final Class<?> targetClass;

    private final Map<String, AdminField> fieldOverrides;
    private final Map<String, AdminField> resolvedFieldsConfigurations = new HashMap<>();
    private final Map<String, ValueFormatter> fieldValueFormatters = new HashMap<>();

    public FieldFactory(
            EntityManager em,
            ConversionService conversionService,
            AutowireCapableBeanFactory beanFactory,
            String modelName,
            AdminModel modelAnnotation,
            EntityType<?> entity,
            @Nullable Class<?> modelAdminClass,
            Class<?> targetClass
    ) {
        this.em = em;
        this.conversionService = conversionService;
        this.beanFactory = beanFactory;

        this.modelName = modelName;
        this.modelAnnotation = modelAnnotation;
        this.entity = entity;
        this.modelAdminClass = modelAdminClass;
        this.adminModelBean = modelAdminClass != null ? beanFactory.createBean(modelAdminClass) : null;
        this.targetClass = targetClass;

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
            if (!fieldConfig.sortBy().equals("")) {
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
        String template = null;
        boolean insertable = true;
        boolean updatable = true;

        AdminField fieldConfig = resolveFieldConfig(name);
        if (fieldConfig != null) {
            label = fieldConfig.label();
            template = fieldConfig.template();
            insertable = fieldConfig.insertable();
            updatable = fieldConfig.updatable();
        }

        if (!StringUtils.hasText(label)) {
            label = AdminStringUtils.toHumanReadableName(name);
        }

        AdminFormFieldValueAccessor formFieldAccessor = resolveFormFieldAccessor(name);

        if (!StringUtils.hasText(template)) {
            template = formFieldAccessor.getDefaultTemplate();
        }

        return new AdminModelField(
                name,
                label,
                template,
                insertable,
                updatable,
                formFieldAccessor
        );
    }

    public AdminModelFilter constructFieldFilter(String name) {
        String label = null;
        AdminField fieldConfig = resolveFieldConfig(name);
        if (fieldConfig != null) {
            label = fieldConfig.label();
        }
        if (!StringUtils.hasText(label)) {
            label = AdminStringUtils.toHumanReadableName(name);
        }

        Attribute<?, ?> attribute = entity.getAttribute(name);
        Attribute.PersistentAttributeType attributeType = attribute.getPersistentAttributeType();
        return switch (attributeType) {
            case ONE_TO_ONE, MANY_TO_ONE -> new AdminModelFilter(
                    name, label,
                    new ToOneFilterPredicateFactory(em, conversionService, attribute),
                    new ToOneFilterOptionsCollector(
                            em, conversionService,
                            entity, attribute,
                            createValueFormatter(name)
                    )
            );
            case ONE_TO_MANY, MANY_TO_MANY -> new AdminModelFilter(
                    name, label,
                    new ToManyFilterPredicateFactory(em, conversionService, attribute),
                    new ToManyFilterOptionsCollector(
                            em, conversionService,
                            entity, attribute,
                            createValueFormatter(name)
                    )
            );
            case BASIC -> new AdminModelFilter(
                    name, label,
                    new BasicFilterPredicateFactory(conversionService, attribute),
                    TypeUtils.isBoolean(attribute.getJavaType())
                            ? new BooleanFilterOptionsCollector(em, entity, attribute)
                            : new BasicFilterOptionsCollector(em, conversionService, entity, attribute)
            );
            default -> throw new IllegalArgumentException(
                    "Unsupported filter attribute type: " + attributeType + " (field " + name + " of model " + modelName + ")"
            );
        };
    }

    @Nullable
    private AdminField resolveFieldConfig(String name) {
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

    @Nullable
    private AdminField discoverFieldConfig(String name) {
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
        AdminModelPropertyReader propertyAccessor = null;
        if (modelAdminClass != null) {
            propertyAccessor = findAdminModelPropertyReader(name);
        }
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

    @Nullable
    private AdminModelPropertyReader findAdminModelPropertyReader(String name) {
        Method method = findModelAdminPropertyReader(modelAdminClass, name, targetClass);
        if (method != null) {
            return new AdminModelDelegatingPropertyReader(name, adminModelBean, method);
        }
        return null;
    }

    @Nullable
    private AdminModelPropertyWriter findAdminModelPropertyWriter(String name) {
        Method method = findModelAdminPropertyWriter(modelAdminClass, name, targetClass);
        if (method != null) {
            return new AdminModelDelegatingPropertyWriter(name, adminModelBean, method);
        }
        return null;
    }

    @Nullable
    private AdminModelPropertyReader findEntityPropertyReader(String name) {
        PropertyDescriptor pd = BeanUtils.getPropertyDescriptor(targetClass, name);
        if (pd != null) {
            return new MethodPropertyReader(pd.getName(), pd.getReadMethod());
        }
        return null;
    }

    @Nullable
    private AdminModelPropertyWriter findEntityPropertyWriter(String name) {
        PropertyDescriptor pd = BeanUtils.getPropertyDescriptor(targetClass, name);
        if (pd != null && pd.getWriteMethod() != null) {
            return new MethodPropertyWriter(pd.getName(), pd.getWriteMethod());
        }
        return null;
    }

    @Nullable
    private AdminModelPropertyReader findEntityFieldReader(String name) {
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

    @Nullable
    private AdminModelPropertyWriter findEntityFieldWriter(String name) {
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

    @Nullable
    private Method findModelAdminPropertyReader(Class<?> modelAdminClass, String name, Class<?> targetClass) {
        Method method = tryFindModelAdminNamedPropertyReader(modelAdminClass, StringUtils.capitalize(name), targetClass);
        if (method == null) {
            method = tryFindModelAdminNamedPropertyReader(modelAdminClass, name, targetClass);
        }
        if (method == null) {
            method = tryFindModelAdminNamedPropertyReader(modelAdminClass, StringUtils.uncapitalize(name), targetClass);
        }
        return method;
    }

    @Nullable
    private Method tryFindModelAdminNamedPropertyReader(
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

    @Nullable
    private Method findModelAdminPropertyWriter(Class<?> modelAdminClass, String name, Class<?> targetClass) {
        Method method = tryFindModelAdminNamedPropertyWriter(modelAdminClass, StringUtils.capitalize(name), targetClass);
        if (method == null) {
            method = tryFindModelAdminNamedPropertyWriter(modelAdminClass, name, targetClass);
        }
        if (method == null) {
            method = tryFindModelAdminNamedPropertyWriter(modelAdminClass, StringUtils.uncapitalize(name), targetClass);
        }
        return method;
    }

    @Nullable
    private Method tryFindModelAdminNamedPropertyWriter(
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

    private AdminFormFieldValueAccessor resolveFormFieldAccessor(String name) {
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

        return constructFormFieldValueAccessor(name, attribute, reader, writer);
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

    @Nullable
    private AdminModelPropertyWriter resolveFormFieldWriter(String name) {
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
            @Nullable Attribute<?, ?> attribute,
            AdminModelPropertyReader reader,
            @Nullable AdminModelPropertyWriter writer
    ) {
        if (attribute != null) {
            Attribute.PersistentAttributeType persistentAttributeType = attribute.getPersistentAttributeType();
            return switch (persistentAttributeType) {
                case MANY_TO_ONE, ONE_TO_ONE ->
                    // TODO: raw id field configuration
                        new ToOneFormFieldAccessor(
                                em, conversionService,
                                attribute, reader, writer, createValueFormatter(name)
                        );
                case MANY_TO_MANY, ONE_TO_MANY -> new ToManyFormFieldAccessor(
                        em, conversionService,
                        attribute, reader, writer, createValueFormatter(name)
                );
                case BASIC -> selectBasicFormFieldAccessor(name, reader, writer);
                default -> throw new IllegalStateException(
                        "Unsupported attribute type: " + persistentAttributeType +
                                " (field " + name + " of model " + modelName + ")"
                );
            };
        }
        return selectBasicFormFieldAccessor(name, reader, writer);
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
                    formatter = new SpelExpressionFormatter(fieldConfiguration.representation());
                }
                fieldValueFormatters.put(
                        fieldName,
                        formatter
                );
            }
        }
        return fieldValueFormatters.get(fieldName);
    }

    private AdminFormFieldValueAccessor selectBasicFormFieldAccessor(
            String name,
            AdminModelPropertyReader reader,
            AdminModelPropertyWriter writer
    ) {
        Class<?> type = reader.getJavaType();
        if (TypeUtils.isBoolean(type)) {
            return new BooleanFormFieldValueAccessor(name, reader, writer);
        }
        return new DelegatingAdminFormFieldValueAccessorImpl(name, conversionService, reader, writer);
    }
}
