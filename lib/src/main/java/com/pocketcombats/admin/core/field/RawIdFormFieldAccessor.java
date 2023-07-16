package com.pocketcombats.admin.core.field;

import com.pocketcombats.admin.core.property.AdminModelPropertyReader;
import com.pocketcombats.admin.core.property.AdminModelPropertyWriter;
import jakarta.annotation.Nullable;
import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.IdentifiableType;
import jakarta.persistence.metamodel.SingularAttribute;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.validation.BindingResult;

import java.util.Collections;
import java.util.Map;

public class RawIdFormFieldAccessor extends AbstractFormFieldValueAccessor
        implements AdminFormFieldSingularValueAccessor {

    private final EntityManager em;
    private final ConversionService conversionService;
    private final boolean optional;

    private final SingularAttribute<?, ?> attribute;
    private final Class<?> attributeIdType;

    public RawIdFormFieldAccessor(
            EntityManager em,
            ConversionService conversionService,
            Attribute<?, ?> attribute,
            boolean optional,
            AdminModelPropertyReader reader,
            @Nullable AdminModelPropertyWriter writer
    ) {
        super(attribute.getName(), reader, writer);

        this.em = em;
        this.conversionService = conversionService;
        this.optional = optional;

        this.attribute = (SingularAttribute<?, ?>) attribute;
        this.attributeIdType = ((IdentifiableType<?>) this.attribute.getType()).getIdType().getJavaType();
    }

    @Override
    public String getDefaultTemplate() {
        return "admin/widget/text";
    }

    @Override
    public Map<String, Object> getModelAttributes() {
        return Collections.emptyMap();
    }

    @Override
    public Object readValue(Object instance) {
        Object reference = getReader().getValue(instance);
        if (reference == null) {
            return "";
        } else {
            return getEntityStringId(reference);
        }
    }

    @Override
    public void setValue(Object instance, @Nullable String value, BindingResult bindingResult) {
        if (StringUtils.isBlank(value)) {
            if (!optional) {
                bindingResult.rejectValue(getName(), "jakarta.validation.constraints.NotNull.message");
            } else {
                getWriter().setValue(instance, null);
            }
        } else {
            Object referenceId;
            try {
                referenceId = conversionService.convert(value, attributeIdType);
            } catch (Exception e) {
                bindingResult.rejectValue(getName(), "spring-jpa-admin.validation.constraints.ValidId.message");
                return;
            }
            Object reference = em.find(attribute.getJavaType(), referenceId);
            if (reference == null) {
                bindingResult.rejectValue(getName(), "spring-jpa-admin.validation.constraints.ValidId.message");
            } else {
                getWriter().setValue(instance, reference);
            }
        }
    }

    protected String getEntityStringId(Object entity) {
        Object id = em.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(entity);
        return conversionService.convert(id, String.class);
    }
}
