package com.pocketcombats.admin.core.field;

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
import org.springframework.core.convert.ConversionService;
import org.springframework.validation.BindingResult;

import java.util.List;
import java.util.Map;

public class ToOneFormFieldAccessor extends AbstractFormFieldValueAccessor {

    private final EntityManager em;
    private final ConversionService conversionService;

    private final Attribute<?, ?> attribute;
    private final Class<?> attributeIdType;

    public ToOneFormFieldAccessor(
            EntityManager em,
            ConversionService conversionService,
            Attribute<?, ?> attribute,
            AdminModelPropertyReader reader,
            @Nullable AdminModelPropertyWriter writer
    ) {
        super(attribute.getName(), reader, writer);

        this.em = em;
        this.conversionService = conversionService;
        this.attribute = attribute;

        this.attributeIdType = em.getEntityManagerFactory().getMetamodel()
                .entity(attribute.getJavaType()).getIdType().getJavaType();
    }

    @Override
    public String getDefaultTemplate() {
        return "admin/widget/to_one_choice";
    }

    @Override
    public Object readValue(Object instance) {
        Object currentValue = getReader().getValue(instance);
        return getEntityStringId(currentValue);
    }

    public Map<String, Object> getModelAttributes(EntityManager em) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        Class<?> attributeJavaType = attribute.getJavaType();
        CriteriaQuery<?> query = cb.createQuery(attributeJavaType);
        Root<?> root = query.from(attributeJavaType);
        Predicate[] restrictions = restrictions(em, cb, root);
        query.where(restrictions);
        List<?> resultList = em.createQuery(query).getResultList();
        return Map.of(
                "_options",
                resultList.stream()
                        .map(entity -> new Option(getEntityStringId(entity), String.valueOf(entity)))
                        .toList()
        );
    }

    protected String getEntityStringId(Object entity) {
        Object id = em.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(entity);
        return conversionService.convert(id, String.class);
    }

    private Predicate[] restrictions(EntityManager em, CriteriaBuilder cb, Root<?> root) {
        return new Predicate[0];
    }

    @Override
    public void setValue(Object instance, String value, BindingResult bindingResult) {
        Object reference = em.getReference(attribute.getJavaType(), conversionService.convert(value, attributeIdType));
        getWriter().setValue(instance, reference);
    }
}
