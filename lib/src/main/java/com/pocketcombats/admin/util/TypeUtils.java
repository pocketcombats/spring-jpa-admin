package com.pocketcombats.admin.util;

import org.springframework.util.ClassUtils;

import java.time.temporal.TemporalAccessor;

public class TypeUtils {

    public static boolean isBasicType(Class<?> type) {
        return ClassUtils.isPrimitiveOrWrapper(type) || TemporalAccessor.class.isAssignableFrom(type);
    }
}
