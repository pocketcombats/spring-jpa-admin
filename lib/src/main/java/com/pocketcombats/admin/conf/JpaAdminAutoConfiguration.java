package com.pocketcombats.admin.conf;

import com.pocketcombats.admin.AdminModel;
import com.pocketcombats.admin.core.AdminModelEntitiesListService;
import com.pocketcombats.admin.core.AdminModelEntitiesListServiceImpl;
import com.pocketcombats.admin.core.AdminModelFormService;
import com.pocketcombats.admin.core.AdminModelFormServiceImpl;
import com.pocketcombats.admin.core.AdminModelRegistry;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.domain.EntityScanner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.Ordered;
import org.springframework.core.convert.ConversionService;

import java.util.Set;

@AutoConfiguration
@EnableConfigurationProperties(JpaAdminProperties.class)
@ComponentScan("com.pocketcombats.admin")
public class JpaAdminAutoConfiguration implements Ordered {

    private static final Logger LOG = LoggerFactory.getLogger(JpaAdminAutoConfiguration.class);

    private final JpaAdminProperties properties;

    public JpaAdminAutoConfiguration(JpaAdminProperties properties) {
        this.properties = properties;
    }

    @Override
    public int getOrder() {
        return properties.getAutoConfigurationOrder();
    }

    @Bean
    @ConditionalOnMissingBean
    public AdminModelRegistry adminModelRegistry(
            ApplicationContext context,
            EntityManager em,
            ConversionService conversionService
    ) throws Exception {
        LOG.debug("Registering default AdminModelRegistry");

        Set<Class<?>> annotatedModels = new EntityScanner(context).scan(AdminModel.class);
        AdminModelRegistryBuilder registryBuilder = new AdminModelRegistryBuilder(
                em,
                context.getAutowireCapableBeanFactory(),
                conversionService
        );
        for (Class<?> annotatedModel : annotatedModels) {
            registryBuilder.addModel(annotatedModel);
        }
        return registryBuilder.build();
    }

    @Bean
    @ConditionalOnMissingBean
    public AdminModelEntitiesListService adminModelEntitiesListService(
            AdminModelRegistry modelRegistry,
            EntityManager em,
            ConversionService conversionService
    ) {
        LOG.debug("Registering default AdminModelEntitiesListService");

        return new AdminModelEntitiesListServiceImpl(
                modelRegistry,
                em,
                conversionService
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public AdminModelFormService adminModelFormService(
            AdminModelRegistry modelRegistry,
            EntityManager em,
            ConversionService conversionService
    ) {
        LOG.debug("Registering default AdminModelFormService");

        return new AdminModelFormServiceImpl(
                modelRegistry,
                em,
                conversionService
        );
    }
}
