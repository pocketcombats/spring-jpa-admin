package com.pocketcombats.admin.conf;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;

/**
 * {@link ClassPathScanningCandidateComponentProvider} that allows abstract classes
 */
/* package */ class AdminModelDefinitionScanner extends ClassPathScanningCandidateComponentProvider {

    public AdminModelDefinitionScanner(boolean useDefaultFilters) {
        super(useDefaultFilters);
    }

    public AdminModelDefinitionScanner(boolean useDefaultFilters, Environment environment) {
        super(useDefaultFilters, environment);
    }

    @Override
    protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
        AnnotationMetadata metadata = beanDefinition.getMetadata();
        return metadata.isIndependent();
    }
}
