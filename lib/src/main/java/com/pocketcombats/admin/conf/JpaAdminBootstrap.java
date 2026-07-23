package com.pocketcombats.admin.conf;

import org.springframework.core.env.Environment;

/**
 * Detects how JPA bootstraps the {@code EntityManagerFactory}, so the admin site can mirror that policy
 * and avoid forcing the JPA metamodel to initialize while the context is still refreshing.
 */
public final class JpaAdminBootstrap {

    public static final String JPA_BOOTSTRAP_PROPERTY = "spring.jpa.bootstrap";

    private JpaAdminBootstrap() {
    }

    /**
     * Whether JPA is configured to bootstrap the {@code EntityManagerFactory} asynchronously
     * ({@code spring.jpa.bootstrap=async}).
     */
    public static boolean isAsyncJpaBootstrap(Environment environment) {
        return "async".equalsIgnoreCase(environment.getProperty(JPA_BOOTSTRAP_PROPERTY));
    }
}
