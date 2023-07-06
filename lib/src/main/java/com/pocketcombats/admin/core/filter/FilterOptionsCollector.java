package com.pocketcombats.admin.core.filter;

import java.util.List;

@FunctionalInterface
public interface FilterOptionsCollector {

    List<ModelFilterOption> collectOptions();
}
