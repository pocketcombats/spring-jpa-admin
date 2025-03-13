package com.pocketcombats.admin.plugin.auth.conf;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("spring.jpa-admin.auth")
public record JpaAdminAuthProperties(
        @DefaultValue("10") int passWordStrength,
        @DefaultValue("false") boolean createDefaultAdmin
) {
}
