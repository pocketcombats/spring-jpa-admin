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
    private final int maxPreloadedOptions;
    private final int maxCountedOptions;
    private final int autocompletePageSize;
    private final boolean methodSecurity;
    private final boolean configureSecurity;
    private final Templates templates;

    public JpaAdminProperties(
            @Nullable Integer autoConfigurationOrder,
            @DefaultValue("false") boolean disableHistory,
            @DefaultValue("10") int historySize,
            @DefaultValue("100") int maxPreloadedOptions,
            @DefaultValue("1000") int maxCountedOptions,
            @DefaultValue("20") int autocompletePageSize,
            @DefaultValue("true") boolean methodSecurity,
            @DefaultValue("true") boolean configureSecurity,
            @DefaultValue Templates templates
    ) {
        if (historySize < 0) {
            throw new IllegalArgumentException(
                    "spring.jpa-admin.history-size must not be negative: " + historySize
            );
        }
        if (maxCountedOptions < 1) {
            throw new IllegalArgumentException(
                    "spring.jpa-admin.max-counted-options must be at least 1: " + maxCountedOptions
            );
        }
        if (autocompletePageSize < 1) {
            throw new IllegalArgumentException(
                    "spring.jpa-admin.autocomplete-page-size must be at least 1: " + autocompletePageSize
            );
        }
        this.autoConfigurationOrder = autoConfigurationOrder == null
                ? Ordered.LOWEST_PRECEDENCE
                : autoConfigurationOrder;
        this.disableHistory = disableHistory;
        this.historySize = historySize;
        this.maxPreloadedOptions = maxPreloadedOptions;
        this.maxCountedOptions = maxCountedOptions;
        this.autocompletePageSize = autocompletePageSize;
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
     * The most options a to-one field preloads into a {@code <select>}. When the target has more
     * rows, the field renders as searchable autocomplete if it can, otherwise a select capped at
     * this many options (plus the current selection) with a "more exist" note. {@code 0} always
     * autocompletes when possible; negative opts out entirely (an uncapped select box).
     */
    public int getMaxPreloadedOptions() {
        return maxPreloadedOptions;
    }

    /**
     * Upper bound on the id probe that computes the "N of M" note for a truncated to-one preload.
     * Once a target table is known to exceed {@link #getMaxPreloadedOptions()}, the total is only
     * counted up to this many rows; past it the note reports "M+" rather than scanning the whole
     * (by definition large) table. Must be at least {@code 1}.
     */
    public int getMaxCountedOptions() {
        return maxCountedOptions;
    }

    public int getAutocompletePageSize() {
        return autocompletePageSize;
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
