package com.pocketcombats.admin.core.filter;

import com.pocketcombats.admin.core.PredicateFactory;
import com.pocketcombats.admin.core.predicate.ValuePredicateFactory;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.util.List;

public class AdminModelFilter {

    private final String name;
    private final String label;
    private final ValuePredicateFactory factory;
    private final FilterOptionsCollector optionsCollector;

    public AdminModelFilter(
            String name,
            String label,
            ValuePredicateFactory factory,
            FilterOptionsCollector optionsCollector
    ) {
        this.name = name;
        this.label = label;
        this.factory = factory;
        this.optionsCollector = optionsCollector;
    }

    public Predicate createPredicate(CriteriaBuilder cb, Root<?> root, String value) {
        return factory.createPredicate(cb, root, value);
    }

    public List<ModelFilterOption> collectOptions(PredicateFactory predicateFactory) {
        return optionsCollector.collectOptions(predicateFactory);
    }

    public String getName() {
        return name;
    }

    public String getLabel() {
        return label;
    }
}
