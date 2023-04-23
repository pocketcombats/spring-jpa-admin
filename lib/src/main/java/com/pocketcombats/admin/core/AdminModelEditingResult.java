package com.pocketcombats.admin.core;

import com.pocketcombats.admin.data.form.EntityDetails;
import org.springframework.validation.BindingResult;

import java.io.Serializable;

public record AdminModelEditingResult(
        EntityDetails entityDetails,
        BindingResult bindingResult
) implements Serializable {

}
