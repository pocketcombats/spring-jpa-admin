package com.pocketcombats.admin;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface AdminFieldOverride {

    String name();

    AdminField field();
}
