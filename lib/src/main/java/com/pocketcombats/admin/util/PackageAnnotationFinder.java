package com.pocketcombats.admin.util;

import jakarta.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;

import java.lang.annotation.Annotation;

public class PackageAnnotationFinder {

    @Nullable
    public static Package findAnnotatedPackage(
            ClassLoader classLoader,
            Class<? extends Annotation> annotationType,
            String packageName
    ) {
        Package aPackage = classLoader.getDefinedPackage(packageName);
        if (aPackage != null) {
            Annotation annotation = aPackage.getAnnotation(annotationType);
            if (annotation != null) {
                return aPackage;
            }
        }

        if (packageName.contains(".")) {
            return findAnnotatedPackage(classLoader, annotationType, StringUtils.substringBeforeLast(packageName, "."));
        } else {
            return null;
        }
    }
}
