package com.pocketcombats.admin;

import org.springframework.stereotype.Indexed;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Indexed
public @interface AdminModel {

    /**
     * Target entity.
     * Has no effect if {@code @AdminModel} is put directly on entity.
     * Otherwise, a valid entity class must be provided.
     */
    Class<?> entity() default Void.class;

    String label() default "";

    String[] listFields() default {};

    int pageSize() default 20;

    String[] searchFields() default {};

    String[] filterFields() default {};

    /**
     * Default order for the list view.
     * Specified field must be {@link AdminField#sortable()}.
     * May begin with "-" (minus) for descending order.
     */
    String defaultOrder() default "";

    AdminFieldset[] fieldsets() default {};

    AdminLink[] links() default {};

    boolean insertable() default true;

    boolean updatable() default true;

    String[] disableActions() default {};

    AdminFieldOverride[] fieldOverrides() default {};
}
