package com.pocketcombats.admin;

public @interface AdminFieldset {

    String label() default "";

    String[] fields();
}
