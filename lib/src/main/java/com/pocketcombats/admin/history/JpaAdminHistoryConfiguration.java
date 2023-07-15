package com.pocketcombats.admin.history;

import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Role;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

/**
 * Attempts to allow {@link AdminHistoryLog} to be auto-scanned.
 * Please note: if you provide {@code @EntityScan} annotation for your application, you have to include
 * {@code "com.pocketcombats.admin.history"} to the list of packages to scan in order to use history log.
 */
@AutoConfiguration
@ConditionalOnProperty(name = "spring.jpa-admin.disable-history", havingValue = "false", matchIfMissing = true)
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@AutoConfigurationPackage(basePackages = "com.pocketcombats.admin.history")
public class JpaAdminHistoryConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(JpaAdminHistoryConfiguration.class);

    private final EntityManager em;

    public JpaAdminHistoryConfiguration(EntityManager em) {
        this.em = em;
    }

    @EventListener
    public void onContextRefresh(ContextRefreshedEvent event) {
        try {
            em.getMetamodel().entity(AdminHistoryLog.class);
        } catch (IllegalArgumentException e) {
            LOG.error(
                    "AdminHistoryLog is not registered as entity. "
                            + "Most likely your app has an explicit @EntityScan annotation. "
                            + "In this case you have to add \"com.pocketcombats.admin.history\" to the list"
                            + " of packages to scan."
            );
            LOG.info("You can disable history by setting spring.jpa-admin.disable-history: true");
            throw e;
        }
    }
}
