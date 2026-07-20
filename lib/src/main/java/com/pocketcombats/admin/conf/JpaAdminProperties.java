package com.pocketcombats.admin.conf;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.core.Ordered;

@ConfigurationProperties(prefix = "spring.jpa-admin")
public class JpaAdminProperties {

    /**
     * Gates {@link JpaAdminMethodSecurityConfiguration}. The auto-configuration reads the property
     * through this constant before binding happens, which is why {@link #isMethodSecurity()} has no
     * callers — the getter documents the property and feeds IDE configuration metadata.
     */
    public static final String METHOD_SECURITY_PROPERTY = "spring.jpa-admin.method-security";

    /**
     * Gates {@link JpaAdminSecurityConfiguration}; see {@link #METHOD_SECURITY_PROPERTY} for why
     * this is a constant rather than a read of {@link #isConfigureSecurity()}.
     */
    public static final String CONFIGURE_SECURITY_PROPERTY = "spring.jpa-admin.configure-security";

    private final int autoConfigurationOrder;
    private final boolean disableHistory;
    private final int historySize;
    private final boolean methodSecurity;
    private final boolean configureSecurity;
    private final Templates templates;

    public JpaAdminProperties(
            @Nullable Integer autoConfigurationOrder,
            @DefaultValue("false") boolean disableHistory,
            @DefaultValue("10") int historySize,
            @DefaultValue("true") boolean methodSecurity,
            @DefaultValue("true") boolean configureSecurity,
            @DefaultValue Templates templates
    ) {
        this.autoConfigurationOrder = autoConfigurationOrder == null
                ? Ordered.LOWEST_PRECEDENCE
                : autoConfigurationOrder;
        this.disableHistory = disableHistory;
        this.historySize = historySize;
        this.methodSecurity = methodSecurity;
        this.configureSecurity = configureSecurity;
        this.templates = templates;
    }


    public int getAutoConfigurationOrder() {
        return autoConfigurationOrder;
    }

    public boolean isDisableHistory() {
        return disableHistory;
    }

    public int getHistorySize() {
        return historySize;
    }

    /**
     * Whether the admin site enables {@code @Secured} method security
     * (see {@link JpaAdminMethodSecurityConfiguration}). Set to {@code false} if the
     * application enables {@code @Secured} support itself.
     */
    public boolean isMethodSecurity() {
        return methodSecurity;
    }

    /**
     * Whether the admin site contributes its own security filter chain configuration
     * (see {@link JpaAdminSecurityConfiguration}). Set to {@code false} to manage
     * security filter chains entirely in the application.
     */
    public boolean isConfigureSecurity() {
        return configureSecurity;
    }

    public Templates getTemplates() {
        return templates;
    }

    public record Templates(
            @DefaultValue("admin/index") String index,
            @DefaultValue("admin/list") String list,
            @DefaultValue("admin/form") String form,
            @DefaultValue("admin/action") String actionPrompt
    ) {
    }
}
