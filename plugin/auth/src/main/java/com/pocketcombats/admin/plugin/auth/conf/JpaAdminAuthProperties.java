package com.pocketcombats.admin.plugin.auth.conf;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("spring.jpa-admin.auth")
public class JpaAdminAuthProperties {

    private int passWordStrength = 10;
    private boolean createDefaultAdmin = true;

    public int getPassWordStrength() {
        return passWordStrength;
    }

    public void setPassWordStrength(int passWordStrength) {
        this.passWordStrength = passWordStrength;
    }

    public boolean isCreateDefaultAdmin() {
        return createDefaultAdmin;
    }

    public void setCreateDefaultAdmin(boolean createDefaultAdmin) {
        this.createDefaultAdmin = createDefaultAdmin;
    }
}
