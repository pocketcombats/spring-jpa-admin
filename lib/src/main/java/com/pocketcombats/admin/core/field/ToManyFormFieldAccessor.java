package com.pocketcombats.admin.core.field;

import com.pocketcombats.admin.core.EntityOptionMapper;
import com.pocketcombats.admin.core.property.AdminModelPropertyReader;
import com.pocketcombats.admin.core.property.AdminModelPropertyWriter;
import com.pocketcombats.admin.util.ConversionUtils;
import com.pocketcombats.admin.widget.Option;
import jakarta.persistence.EntityManager;
import jakarta.persistence.IdClass;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.IdentifiableType;
import jakarta.persistence.metamodel.PluralAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import org.jspecify.annotations.Nullable;
import org.springframework.core.CollectionFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.validation.BindingResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ToManyFormFieldAccessor extends AbstractFormFieldValueAccessor
        implements AdminFormFieldPluralValueAccessor {

    private final EntityManager em;
    private final ConversionService conversionService;
    private final EntityOptionMapper optionMapper;

    private final Class<?> attributeElementJavaType;
    private final Class<?> attributeElementIdType;
    // Name of the element's single basic id attribute, enabling a single batched existence query;
    // null for a composite/embedded id, which falls back to per-id lookups.
    private final @Nullable String attributeElementIdName;

    public ToManyFormFieldAccessor(
            EntityManager em,
            ConversionService conversionService,
            PluralAttribute<?, ?, ?> attribute,
            AdminModelPropertyReader reader,
            @Nullable AdminModelPropertyWriter writer,
            EntityOptionMapper optionMapper
    ) {
        super(attribute.getName(), reader, writer);

        this.em = em;
        this.conversionService = conversionService;
        this.optionMapper = optionMapper;

        IdentifiableType<?> elementType = (IdentifiableType<?>) attribute.getElementType();
        this.attributeElementJavaType = elementType.getJavaType();
        this.attributeElementIdType = elementType.getIdType().getJavaType();
        this.attributeElementIdName = resolveBatchableIdName(elementType, attributeElementIdType);
    }

    /**
     * Only a basic, single-column id can be matched with a single {@code id IN (:ids)} query.
     * Composite ({@link IdClass @IdClass}) and embedded ({@link jakarta.persistence.EmbeddedId @EmbeddedId})
     * ids return {@code null} and fall back to per-id lookups.
     */
    private static @Nullable String resolveBatchableIdName(IdentifiableType<?> elementType, Class<?> idType) {
        if (!elementType.hasSingleIdAttribute()) {
            return null;
        }
        SingularAttribute<?, ?> id = elementType.getId(idType);
        return id.getPersistentAttributeType() == Attribute.PersistentAttributeType.BASIC ? id.getName() : null;
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
                .map(optionMapper::stringId)
                .toList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setValues(Object instance, @Nullable List<String> values, BindingResult bindingResult) {
        // Resolve every id to an existing entity before touching the collection: a malformed or
        // stale id (e.g., an option deleted since the form rendered) must produce a field error and
        // leave the current relation state untouched, rather than fail later at flush.
        List<Object> ids = new ArrayList<>(values == null ? 0 : values.size());
        if (values != null) {
            for (String value : values) {
                Object id = ConversionUtils.tryConvert(conversionService, value, attributeElementIdType);
                if (id == null) {
                    bindingResult.rejectValue(getName(), "spring-jpa-admin.validation.constraints.ValidId.message");
                    return;
                }
                ids.add(id);
            }
        }
        List<Object> references = resolveReferences(ids);
        if (references == null) {
            bindingResult.rejectValue(getName(), "spring-jpa-admin.validation.constraints.ValidId.message");
            return;
        }
        Collection<Object> attributeRef = (Collection<Object>) getReader().getValue(instance);
        if (attributeRef == null) {
            attributeRef = CollectionFactory.createCollection(getReader().getJavaType(), references.size());
            getWriter().setValue(instance, attributeRef);
        } else {
            attributeRef.clear();
        }
        attributeRef.addAll(references);
    }

    /**
     * Managed references for all submitted ids, in submission order, or {@code null} when any id no longer
     * exists.
     * A single query checks the existence for basic ids; composite ids fall back to per-id find.
     */
    private @Nullable List<Object> resolveReferences(List<Object> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        String idName = attributeElementIdName;
        if (idName == null) {
            List<Object> references = new ArrayList<>(ids.size());
            for (Object id : ids) {
                Object reference = em.find(attributeElementJavaType, id);
                if (reference == null) {
                    return null;
                }
                references.add(reference);
            }
            return references;
        }
        if (!allIdsExist(idName, ids)) {
            return null;
        }
        List<Object> references = new ArrayList<>(ids.size());
        for (Object id : ids) {
            references.add(em.getReference(attributeElementJavaType, id));
        }
        return references;
    }

    /**
     * Whether every submitted id still exists, via a single id-only count query.
     */
    private boolean allIdsExist(String idName, Collection<Object> ids) {
        // Dedupe first
        Set<Object> distinctIds = new HashSet<>(ids);

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<?> root = query.from(attributeElementJavaType);
        var idPath = root.get(idName);
        query.select(cb.count(idPath)).where(idPath.in(distinctIds));
        long matched = em.createQuery(query).getSingleResult();

        return matched == distinctIds.size();
    }

    @Override
    public Map<String, Object> getModelAttributes(Object instance) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<?> query = cb.createQuery(attributeElementJavaType);
        Root<?> root = query.from(attributeElementJavaType);
        // Order by the element's basic id so the option list is stable across renders; a
        // composite/embedded id (null) simply isn't explicitly ordered, as in ToOneFormFieldAccessor.
        if (attributeElementIdName != null) {
            query.orderBy(cb.asc(root.get(attributeElementIdName)));
        }
        List<?> resultList = em.createQuery(query).getResultList();

        return Map.of(
                "_options",
                resultList.stream()
                        .map(entity -> new Option(optionMapper.stringId(entity), optionMapper.label(entity)))
                        .toList()
        );
    }
}
