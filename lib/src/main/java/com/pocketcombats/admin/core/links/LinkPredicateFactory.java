package com.pocketcombats.admin.core.links;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

public class LinkPredicateFactory {

    private final EntityManager em;
    private final String mappedBy;

    public LinkPredicateFactory(EntityManager em, String mappedBy) {
        this.em = em;
        this.mappedBy = mappedBy;
    }

    public Predicate createPredicate(Object value, Root<?> root) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        return cb.equal(root.get(mappedBy), value);
    }
}
