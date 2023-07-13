package com.pocketcombats.admin.core.field;

import com.pocketcombats.admin.core.formatter.ValueFormatter;
import com.pocketcombats.admin.core.property.AdminModelPropertyReader;
import com.pocketcombats.admin.core.property.AdminModelPropertyWriter;
import com.pocketcombats.admin.widget.Option;
import jakarta.annotation.Nullable;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.IdentifiableType;
import jakarta.persistence.metamodel.PluralAttribute;
import org.springframework.core.CollectionFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.validation.BindingResult;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ToManyFormFieldAccessor extends AbstractFormFieldValueAccessor
        implements AdminFormFieldPluralValueAccessor {

    private final EntityManager em;
    private final ConversionService conversionService;
    private final ValueFormatter valueFormatter;

    private final Class<?> attributeElementJavaType;
    private final Class<?> attributeElementIdType;

    public ToManyFormFieldAccessor(
            EntityManager em,
            ConversionService conversionService,
            Attribute<?, ?> attribute,
            AdminModelPropertyReader reader,
            @Nullable AdminModelPropertyWriter writer,
            ValueFormatter valueFormatter
    ) {
        super(attribute.getName(), reader, writer);

        this.em = em;
        this.conversionService = conversionService;
        this.valueFormatter = valueFormatter;

        IdentifiableType<?> elementType = (IdentifiableType<?>) ((PluralAttribute<?, ?, ?>) attribute).getElementType();
        this.attributeElementJavaType = elementType.getJavaType();
        this.attributeElementIdType = elementType.getIdType().getJavaType();
    }

    @Override
    public String getDefaultTemplate() {
        return "admin/widget/multiselect";
    }

    @Override
    public List<String> readValue(Object instance) {
        Collection<?> value = (Collection<?>) getReader().getValue(instance);
        if (value == null) {
            return Collections.emptyList();
        }
        return value.stream()
                .map(this::getEntityStringId)
                .toList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setValues(Object instance, @Nullable List<String> values, BindingResult bindingResult) {
        Collection<Object> attributeRef = (Collection<Object>) getReader().getValue(instance);
        if (attributeRef == null) {
            attributeRef = CollectionFactory.createCollection(
                    getReader().getJavaType(),
                    values == null ? 0 : values.size()
            );
            getWriter().setValue(instance, attributeRef);
        } else {
            attributeRef.clear();
        }
        if (values != null) {
            List<?> valueRefs = values.stream()
                    .map(value -> em.getReference(
                            attributeElementJavaType,
                            conversionService.convert(value, attributeElementIdType))
                    )
                    .toList();
            attributeRef.addAll(valueRefs);
        }
    }

    @Override
    public Map<String, Object> getModelAttributes() {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<?> query = cb.createQuery(attributeElementJavaType);
        Root<?> root = query.from(attributeElementJavaType);
        List<?> resultList = em.createQuery(query).getResultList();

        return Map.of(
                "_options",
                resultList.stream()
                        .map(entity -> new Option(getEntityStringId(entity), getEntityStringValue(entity)))
                        .toList()
        );
    }

    protected String getEntityStringId(Object entity) {
        Object id = em.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(entity);
        return conversionService.convert(id, String.class);
    }

    protected String getEntityStringValue(Object entity) {
        return valueFormatter.format(entity);
    }

}
