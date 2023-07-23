package com.pocketcombats.admin.conf;

import com.pocketcombats.admin.AdminModel;
import com.pocketcombats.admin.AdminUrlParamsHelper;
import com.pocketcombats.admin.core.AdminModelEntitiesListService;
import com.pocketcombats.admin.core.AdminModelEntitiesListServiceImpl;
import com.pocketcombats.admin.core.AdminModelFormService;
import com.pocketcombats.admin.core.AdminModelFormServiceImpl;
import com.pocketcombats.admin.core.AdminModelListEntityMapper;
import com.pocketcombats.admin.core.AdminModelRegistry;
import com.pocketcombats.admin.core.action.AdminModelAction;
import com.pocketcombats.admin.core.action.AdminModelActionService;
import com.pocketcombats.admin.core.action.AdminModelActionServiceImpl;
import com.pocketcombats.admin.core.action.DefaultDeleteAction;
import com.pocketcombats.admin.core.formatter.SpelExpressionContextFactory;
import com.pocketcombats.admin.core.links.AdminModelLinkFactory;
import com.pocketcombats.admin.core.links.AdminRelationLinkService;
import com.pocketcombats.admin.history.AdminHistoryCompiler;
import com.pocketcombats.admin.history.AdminHistoryCompilerImpl;
import com.pocketcombats.admin.history.AdminHistoryWriter;
import com.pocketcombats.admin.history.AdminHistoryWriterImpl;
import com.pocketcombats.admin.history.NoOpAdminHistoryCompiler;
import com.pocketcombats.admin.history.NoOpAdminHistoryWriter;
import com.pocketcombats.admin.web.IndexController;
import com.pocketcombats.admin.web.ModelActionController;
import com.pocketcombats.admin.web.ModelFormController;
import com.pocketcombats.admin.web.ModelListController;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.domain.EntityScanner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.convert.ConversionService;
import org.springframework.validation.SmartValidator;

import java.util.List;
import java.util.Set;

@AutoConfiguration
@EnableConfigurationProperties(JpaAdminProperties.class)
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
            ConversionService conversionService,
            SpelExpressionContextFactory spelExpressionContextFactory,
            AdminModelLinkFactory linksFactory,
            ActionsFactory actionsFactory
    ) throws Exception {
        LOG.debug("Registering default AdminModelRegistry");

        Set<Class<?>> annotatedModels = new EntityScanner(context).scan(AdminModel.class);
        AdminModelRegistryBuilder registryBuilder = new AdminModelRegistryBuilder(
                em,
                context.getAutowireCapableBeanFactory(),
                conversionService,
                spelExpressionContextFactory,
                linksFactory,
                actionsFactory
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
            ConversionService conversionService,
            AdminModelListEntityMapper mapper
    ) {
        LOG.debug("Registering default AdminModelEntitiesListService");

        return new AdminModelEntitiesListServiceImpl(
                modelRegistry,
                em,
                conversionService,
                mapper
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public AdminModelFormService adminModelFormService(
            AdminModelRegistry modelRegistry,
            AdminHistoryWriter historyWriter,
            AdminRelationLinkService relationLinkService,
            EntityManager em,
            SmartValidator validator,
            ConversionService conversionService
    ) {
        LOG.debug("Registering default AdminModelFormService");

        return new AdminModelFormServiceImpl(
                modelRegistry,
                historyWriter,
                relationLinkService,
                em,
                validator,
                conversionService
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public AdminModelActionService adminModelActionService(
            AdminModelRegistry modelRegistry,
            AdminModelListEntityMapper mapper,
            EntityManager em,
            ConversionService conversionService
    ) {
        LOG.debug("Registering default AdminModelActionService");

        return new AdminModelActionServiceImpl(modelRegistry, mapper, em, conversionService);
    }

    @Bean
    @ConditionalOnMissingBean
    public AdminHistoryWriter adminHistoryWriter(
            AdminModelListEntityMapper mapper,
            EntityManager em,
            ConversionService conversionService
    ) {
        if (properties.isDisableHistory()) {
            return new NoOpAdminHistoryWriter();
        } else {
            LOG.debug("Registering default AdminHistoryWriter");

            return new AdminHistoryWriterImpl(mapper, em, conversionService);
        }
    }

    @Bean
    @ConditionalOnMissingBean
    public AdminHistoryCompiler historyCompiler(
            AdminModelRegistry registry,
            EntityManager em
    ) {
        if (properties.isDisableHistory()) {
            return new NoOpAdminHistoryCompiler();
        } else {
            LOG.debug("Registering default AdminHistoryCompiler");

            return new AdminHistoryCompilerImpl(registry, em);
        }
    }

    @Bean
    public AdminRelationLinkService adminRelationLinkService(
            AdminModelRegistry modelRegistry,
            AdminModelListEntityMapper mapper,
            EntityManager em,
            ConversionService conversionService
    ) {
        return new AdminRelationLinkService(modelRegistry, mapper, em, conversionService);
    }

    @Bean
    @ConditionalOnMissingBean
    public SpelExpressionContextFactory spelExpressionContextFactory() {
        LOG.debug("Registering default SpelExpressionContextFactory");

        return new SpelExpressionContextFactory();
    }

    @Bean
    public AdminModelLinkFactory adminModelLinkFactory(EntityManager em) {
        return new AdminModelLinkFactory(em);
    }

    @Bean
    public ActionsFactory adminActionsFactory(AdminHistoryWriter historyWriter, List<AdminModelAction> defaultActions) {
        return new ActionsFactory(historyWriter, defaultActions);
    }

    @Bean
    public AdminUrlParamsHelper adminUrlParamsHelper() {
        return new AdminUrlParamsHelper();
    }

    @Bean
    public AdminModelListEntityMapper adminModelListEntityMapper(
            EntityManager em,
            ConversionService conversionService
    ) {
        return new AdminModelListEntityMapper(em, conversionService);
    }

    @Bean
    @ConditionalOnMissingBean(name = "adminDeleteAction")
    public DefaultDeleteAction adminDeleteAction(AdminHistoryWriter historyWriter) {
        return new DefaultDeleteAction(historyWriter);
    }

    @Bean
    public IndexController adminIndexController(
            AdminModelRegistry entityRegistry,
            AdminHistoryCompiler historyCompiler
    ) {
        return new IndexController(properties, entityRegistry, historyCompiler);
    }

    @Bean
    public ModelListController adminModelListController(
            AdminModelEntitiesListService entitiesListService,
            AdminRelationLinkService relationLinkService
    ) {
        return new ModelListController(properties, entitiesListService, relationLinkService);
    }

    @Bean
    public ModelFormController adminModelFormController(AdminModelFormService service) {
        return new ModelFormController(properties, service);
    }

    @Bean
    public ModelActionController adminModelActionController(AdminModelActionService service) {
        return new ModelActionController(properties, service);
    }
}
