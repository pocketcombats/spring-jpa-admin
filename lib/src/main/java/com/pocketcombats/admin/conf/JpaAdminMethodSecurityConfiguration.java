package com.pocketcombats.admin.conf;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Enables {@code @Secured} method security, which enforces the role checks on all admin endpoints.
 * Gated separately from {@link JpaAdminSecurityConfiguration} so that applications disabling
 * {@code spring.jpa-admin.configure-security} to manage their own filter chains keep method-level
 * authorization. Disable via {@code spring.jpa-admin.method-security} only if the application
 * enables {@code @Secured} support itself.
 */
@AutoConfiguration
@ConditionalOnProperty(name = JpaAdminProperties.METHOD_SECURITY_PROPERTY, havingValue = "true", matchIfMissing = true)
@EnableMethodSecurity(securedEnabled = true)
public class JpaAdminMethodSecurityConfiguration {
}
