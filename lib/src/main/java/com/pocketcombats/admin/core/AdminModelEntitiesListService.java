package com.pocketcombats.admin.core;

import com.pocketcombats.admin.data.list.AdminModelEntitiesList;
import com.pocketcombats.admin.data.list.ModelRequest;

public interface AdminModelEntitiesListService {

    AdminModelEntitiesList listEntities(String modelName, ModelRequest query) throws UnknownModelException;
}
