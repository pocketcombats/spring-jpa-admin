package com.pocketcombats.admin.core;

import com.pocketcombats.admin.data.list.AdminModelEntitiesList;
import com.pocketcombats.admin.data.list.ModelRequest;

import java.util.Map;

public interface AdminModelEntitiesListService {

    AdminModelEntitiesList listEntities(
            String modelName,
            ModelRequest query,
            Map<String, String> filters
    ) throws UnknownModelException;
}
