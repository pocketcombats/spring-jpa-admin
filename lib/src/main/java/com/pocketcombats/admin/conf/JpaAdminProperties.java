package com.pocketcombats.admin.conf;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.Ordered;

@ConfigurationProperties(prefix = "spring.jpa-admin")
public class JpaAdminProperties {

    private int autoConfigurationOrder = Ordered.LOWEST_PRECEDENCE;

    public int getAutoConfigurationOrder() {
        return autoConfigurationOrder;
    }

    public void setAutoConfigurationOrder(int autoConfigurationOrder) {
        this.autoConfigurationOrder = autoConfigurationOrder;
    }
}
