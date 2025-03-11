package com.pocketcombats.admin.core.uniqueness;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.util.List;

public interface AdminUniqueConstraint {

    Predicate createPredicate(CriteriaBuilder cb, Root<?> root, Object entity);

    boolean matches(Object entityA, Object entityB);

    List<String> getFieldLabels();
}
