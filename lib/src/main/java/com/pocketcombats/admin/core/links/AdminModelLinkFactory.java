package com.pocketcombats.admin.core.links;

import com.pocketcombats.admin.AdminLink;
import com.pocketcombats.admin.AdminModel;
import com.pocketcombats.admin.core.formatter.SpelExpressionContextFactory;
import com.pocketcombats.admin.core.formatter.SpelExpressionFormatter;
import com.pocketcombats.admin.core.formatter.ValueFormatter;
import jakarta.annotation.Nullable;
import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EntityType;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class AdminModelLinkFactory {

    private static final Logger LOG = LoggerFactory.getLogger(AdminModelLinkFactory.class);

    private final EntityManager em;
    private final SpelExpressionContextFactory spelExpressionContextFactory;

    public AdminModelLinkFactory(
            EntityManager em,
            SpelExpressionContextFactory spelExpressionContextFactory
    ) {
        this.em = em;
        this.spelExpressionContextFactory = spelExpressionContextFactory;
    }

    public List<AdminModelLink> createModelLinks(
            String modelName,
            AdminModel modelAnnotation,
            Class<?> modelType
    ) {
        return Arrays.stream(modelAnnotation.links())
                .map(linkAnnotation -> create(modelName, modelType, linkAnnotation))
                .toList();
    }

    private AdminModelLink create(String modelName, Class<?> modelType, AdminLink linkAnnotation) {
        if (linkAnnotation.preview() < 1 && StringUtils.isNotEmpty(linkAnnotation.sortBy())) {
            LOG.debug(
                    "Link {} of model {} specifies order, but it will have no effect",
                    linkAnnotation, modelName
            );
        }
        EntityType<?> targetEntity = em.getMetamodel().entity(linkAnnotation.target());
        ValueFormatter formatter;
        if (linkAnnotation.representation().isEmpty()) {
            formatter = null;
        } else {
            formatter = new SpelExpressionFormatter(
                    spelExpressionContextFactory,
                    linkAnnotation.representation()
            );
        }
        return new AdminModelLink(
                linkAnnotation.label(),
                linkAnnotation.target(),
                createPredicateFactory(modelType, targetEntity, linkAnnotation),
                linkAnnotation.preview(),
                formatter,
                linkAnnotation.sortBy(),
                createOrderFactory(targetEntity, linkAnnotation.sortBy())
        );
    }

    @SuppressWarnings("unchecked")
    private LinkPredicateFactory createPredicateFactory(
            Class<?> modelType,
            EntityType<?> entity,
            AdminLink linkAnnotation
    ) {
        Attribute<?, ?> mappedAttribute;
        if (!"".equals(linkAnnotation.mappedBy())) {
            mappedAttribute = entity.getAttribute(linkAnnotation.mappedBy());
        } else {
            Set<Attribute<?, ?>> attributes = (Set<Attribute<?, ?>>) entity.getAttributes();
            List<Attribute<?, ?>> candidateAttributes = attributes.stream()
                    .filter(candidateAttribute -> candidateAttribute.getJavaType().isAssignableFrom(modelType))
                    .toList();
            if (candidateAttributes.isEmpty()) {
                throw new IllegalStateException(
                        "Can't find *-to-one relation from " + entity.getName() + " to "
                                + modelType.getSimpleName()
                );
            }
            if (candidateAttributes.size() > 1) {
                throw new IllegalStateException(
                        "Found more than 1 reference from " + entity.getName() + " to "
                                + modelType.getSimpleName() + " (actual count: " + candidateAttributes.size() + "). "
                                + "Please provide explicit \"mappedBy\" for admin link."
                );
            }
            mappedAttribute = candidateAttributes.get(0);
        }
        return new LinkPredicateFactory(em, mappedAttribute.getName());
    }

    private @Nullable LinkOrderFactory createOrderFactory(EntityType<?> entity, String order) {
        if (StringUtils.isEmpty(order)) {
            return null;
        }

        Attribute<?, ?> attribute;
        boolean asc;
        if (order.startsWith("-")) {
            asc = false;
            attribute = entity.getAttribute(order.substring(1));
        } else {
            asc = true;
            attribute = entity.getAttribute(order);
        }
        return new LinkOrderFactory(em, attribute.getName(), asc);
    }
}
