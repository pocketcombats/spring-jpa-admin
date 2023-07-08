package com.pocketcombats.admin.core.field;

import com.pocketcombats.admin.core.formatter.ValueFormatter;
import com.pocketcombats.admin.core.property.AdminModelPropertyReader;
import com.pocketcombats.admin.core.property.AdminModelPropertyWriter;
import com.pocketcombats.admin.widget.Option;
import jakarta.annotation.Nullable;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.IdentifiableType;
import jakarta.persistence.metamodel.SingularAttribute;
import org.springframework.core.convert.ConversionService;
import org.springframework.validation.BindingResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ToOneFormFieldAccessor extends AbstractFormFieldValueAccessor
        implements AdminFormFieldSingularValueAccessor {

    private static final String ID_PREFIX = "id";

    private final EntityManager em;
    private final ConversionService conversionService;
    private final boolean optional;
    private final ValueFormatter valueFormatter;

    private final SingularAttribute<?, ?> attribute;
    private final Class<?> attributeIdType;

    public ToOneFormFieldAccessor(
            EntityManager em,
            ConversionService conversionService,
            Attribute<?, ?> attribute,
            boolean optional,
            AdminModelPropertyReader reader,
            @Nullable AdminModelPropertyWriter writer,
            ValueFormatter valueFormatter
    ) {
        super(attribute.getName(), reader, writer);

        this.em = em;
        this.conversionService = conversionService;
        this.attribute = (SingularAttribute<?, ?>) attribute;
        this.optional = optional;
        this.valueFormatter = valueFormatter;

        this.attributeIdType = ((IdentifiableType<?>) this.attribute.getType()).getIdType().getJavaType();
    }

    @Override
    public String getDefaultTemplate() {
        return "admin/widget/to_one_choice";
    }

    @Override
    public String readValue(Object instance) {
        Object currentValue = getReader().getValue(instance);
        return getEntityStringId(currentValue);
    }

    @Override
    public Map<String, Object> getModelAttributes() {
        List<Option> valueOptions = collectValueOptions();
        List<Option> options;
        if (optional) {
            options = new ArrayList<>(valueOptions.size() + 1);
            options.add(Option.EMPTY);
            options.addAll(valueOptions);
        } else {
            options = valueOptions;
        }
        return Map.of("_options", options);
    }

    private List<Option> collectValueOptions() {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        Class<?> attributeJavaType = attribute.getJavaType();
        CriteriaQuery<?> query = cb.createQuery(attributeJavaType);
        Root<?> root = query.from(attributeJavaType);
        Predicate[] restrictions = restrictions(em, cb, root);
        query.where(restrictions);
        List<?> resultList = em.createQuery(query).getResultList();
        return resultList.stream()
                .map(entity -> new Option(getEntityStringId(entity), getEntityStringValue(entity)))
                .toList();
    }

    protected String getEntityStringId(Object entity) {
        if (entity == null) {
            return Option.EMPTY.id();
        } else {
            Object id = em.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(entity);
            return ID_PREFIX + conversionService.convert(id, String.class);
        }
    }

    protected String getEntityStringValue(Object entity) {
        return valueFormatter.format(entity);
    }

    private Predicate[] restrictions(EntityManager em, CriteriaBuilder cb, Root<?> root) {
        return new Predicate[0];
    }

    @Override
    public void setValue(Object instance, String value, BindingResult bindingResult) {
        if (Option.EMPTY.id().equals(value)) {
            getWriter().setValue(instance, null);
        } else {
            Object referenceId = conversionService.convert(value.substring(ID_PREFIX.length()), attributeIdType);
            Object reference = em.getReference(attribute.getJavaType(), referenceId);
            getWriter().setValue(instance, reference);
        }
    }
}
