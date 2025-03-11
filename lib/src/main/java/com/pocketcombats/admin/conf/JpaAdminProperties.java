package com.pocketcombats.admin.conf;

import jakarta.annotation.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.core.Ordered;

@ConfigurationProperties(prefix = "spring.jpa-admin")
public class JpaAdminProperties {

    private final int autoConfigurationOrder;
    private final boolean disableHistory;
    private final int historySize;
    private final Templates templates;

    public JpaAdminProperties(
            @Nullable Integer autoConfigurationOrder,
            @DefaultValue("false") boolean disableHistory,
            @DefaultValue("10") int historySize,
            @DefaultValue Templates templates
    ) {
        this.autoConfigurationOrder = autoConfigurationOrder == null
                ? Ordered.LOWEST_PRECEDENCE
                : autoConfigurationOrder;
        this.disableHistory = disableHistory;
        this.historySize = historySize;
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
