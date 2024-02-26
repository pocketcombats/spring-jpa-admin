package com.pocketcombats.admin.conf;

import org.springframework.boot.autoconfigure.domain.EntityScanner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;

/* package */ class AdminModelScanner extends EntityScanner {

    public AdminModelScanner(ApplicationContext context) {
        super(context);
    }

    @Override
    protected ClassPathScanningCandidateComponentProvider createClassPathScanningCandidateComponentProvider(
            ApplicationContext context
    ) {
        AdminModelDefinitionScanner scanner = new AdminModelDefinitionScanner(false);
        scanner.setEnvironment(context.getEnvironment());
        scanner.setResourceLoader(context);
        return scanner;
    }
}
