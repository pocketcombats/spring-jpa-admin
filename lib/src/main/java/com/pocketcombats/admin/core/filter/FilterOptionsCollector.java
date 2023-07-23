package com.pocketcombats.admin.core.filter;

import com.pocketcombats.admin.core.PredicateFactory;

import java.util.List;

@FunctionalInterface
public interface FilterOptionsCollector {

    List<ModelFilterOption> collectOptions(PredicateFactory predicateFactory);
}
