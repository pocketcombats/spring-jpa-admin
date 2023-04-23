package com.pocketcombats.admin;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AdminModel {

    Class<?> model() default Void.class;

    String[] listFields() default {};

    int pageSize() default 20;

    String[] searchFields() default {};

    String[] filterFields() default {};

    String[] sortFields() default {};

    AdminFieldset[] fieldsets() default {};

    Action[] permittedActions() default {Action.EDIT, Action.CREATE, Action.DELETE};

    AdminFieldOverride[] fieldOverrides() default {};
}
