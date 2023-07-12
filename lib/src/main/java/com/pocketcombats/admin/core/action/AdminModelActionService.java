package com.pocketcombats.admin.core.action;

import com.pocketcombats.admin.core.UnknownModelException;
import com.pocketcombats.admin.data.action.ActionPrompt;

import java.util.List;

public interface AdminModelActionService {

    ActionPrompt prompt(String model, String action, List<String> ids)
            throws UnknownModelException, UnknownActionException;

    void perform(String model, String action, List<String> ids)
            throws UnknownModelException, UnknownActionException;
}
