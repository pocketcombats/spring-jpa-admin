package com.pocketcombats.admin.core.links;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Root;

public class LinkOrderFactory {

    private final String attribute;
    private final boolean asc;
    private final CriteriaBuilder cb;

    public LinkOrderFactory(EntityManager em, String attribute, boolean asc) {
        this.attribute = attribute;
        this.asc = asc;
        this.cb = em.getCriteriaBuilder();
    }

    public Order create(Root<?> root) {
        if (asc) {
            return cb.asc(root.get(attribute));
        } else {
            return cb.desc(root.get(attribute));
        }
    }
}
