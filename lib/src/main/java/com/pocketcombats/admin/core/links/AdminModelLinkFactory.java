package com.pocketcombats.admin.core.links;

import com.pocketcombats.admin.AdminLink;
import com.pocketcombats.admin.AdminModel;
import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EntityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class AdminModelLinkFactory {

    private static final Logger LOG = LoggerFactory.getLogger(AdminModelLinkFactory.class);

    private final EntityManager em;

    public AdminModelLinkFactory(EntityManager em) {
        this.em = em;
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
        if (linkAnnotation.preview() < 1 && !"".equals(linkAnnotation.sortBy())) {
            LOG.debug(
                    "Link {} of model {} specifies order, but it will have no effect",
                    linkAnnotation, modelName
            );
        }
        EntityType<?> targetEntity = em.getMetamodel().entity(linkAnnotation.target());
        return new AdminModelLink(
                linkAnnotation.label(),
                linkAnnotation.target(),
                createPredicateFactory(modelType, targetEntity, linkAnnotation),
                linkAnnotation.preview(),
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

    private LinkOrderFactory createOrderFactory(EntityType<?> entity, String order) {
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
