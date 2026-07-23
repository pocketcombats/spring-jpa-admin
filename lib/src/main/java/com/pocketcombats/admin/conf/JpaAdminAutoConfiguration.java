package com.pocketcombats.admin.conf;

import com.pocketcombats.admin.AdminModel;
import com.pocketcombats.admin.core.*;
import com.pocketcombats.admin.core.action.AdminModelAction;
import com.pocketcombats.admin.core.action.AdminModelActionService;
import com.pocketcombats.admin.core.action.AdminModelActionServiceImpl;
import com.pocketcombats.admin.core.action.DefaultDeleteAction;
import com.pocketcombats.admin.core.formatter.SpelExpressionContextFactory;
import com.pocketcombats.admin.core.links.AdminModelLinkFactory;
import com.pocketcombats.admin.core.links.AdminRelationLinkService;
import com.pocketcombats.admin.core.permission.AdminPermissionService;
import com.pocketcombats.admin.core.permission.AdminPermissionServiceImpl;
import com.pocketcombats.admin.history.AdminHistoryCompiler;
import com.pocketcombats.admin.history.AdminHistoryCompilerImpl;
import com.pocketcombats.admin.history.AdminHistoryWriter;
import com.pocketcombats.admin.history.AdminHistoryWriterImpl;
import com.pocketcombats.admin.history.NoOpAdminHistoryCompiler;
import com.pocketcombats.admin.history.NoOpAdminHistoryWriter;
import com.pocketcombats.admin.thymeleaf.AdminUrlParamsHelper;
import com.pocketcombats.admin.thymeleaf.MessageHelper;
import com.pocketcombats.admin.web.IndexController;
import com.pocketcombats.admin.web.ModelActionController;
import com.pocketcombats.admin.web.ModelFieldOptionsController;
import com.pocketcombats.admin.web.ModelFormController;
import com.pocketcombats.admin.web.ModelListController;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.env.Environment;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.validation.SmartValidator;
import org.thymeleaf.extras.springsecurity6.dialect.SpringSecurityDialect;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

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
            Environment environment,
            ApplicationContext context,
            EntityManager em,
            ConversionService conversionService,
            SpelExpressionContextFactory spelExpressionContextFactory,
            AdminModelLinkFactory linksFactory,
            ActionsFactory actionsFactory
    ) {
        Supplier<AdminModelRegistry> factory = () -> buildRegistry(
                context, em, conversionService, spelExpressionContextFactory, linksFactory, actionsFactory
        );
        if (JpaAdminBootstrap.isAsyncJpaBootstrap(environment)) {
            // spring.jpa.bootstrap=async builds the EntityManagerFactory on a background thread; reading
            // the metamodel now would force it to finish and defeat that. Build the registry lazily,
            // off the refresh thread, and warm it up once the application is ready.
            LOG.debug("Registering deferred AdminModelRegistry (spring.jpa.bootstrap=async)");
            SimpleAsyncTaskExecutor warmUpExecutor = new SimpleAsyncTaskExecutor("jpa-admin-warmup-");
            warmUpExecutor.setDaemon(true);
            return new DeferredAdminModelRegistry(factory, warmUpExecutor);
        }
        LOG.debug("Registering default AdminModelRegistry");
        return factory.get();
    }

    private AdminModelRegistry buildRegistry(
            ApplicationContext context,
            EntityManager em,
            ConversionService conversionService,
            SpelExpressionContextFactory spelExpressionContextFactory,
            AdminModelLinkFactory linksFactory,
            ActionsFactory actionsFactory
    ) {
        Set<Class<?>> annotatedModels;
        try {
            annotatedModels = new AdminModelScanner(context).scan(AdminModel.class);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Failed to scan for @AdminModel classes", e);
        }
        AdminModelRegistryBuilder registryBuilder = new AdminModelRegistryBuilder(
                em,
                context.getAutowireCapableBeanFactory(),
                conversionService,
                spelExpressionContextFactory,
                linksFactory,
                actionsFactory,
                properties.getMaxPreloadedOptions(),
                properties.getMaxCountedOptions()
        );
        for (Class<?> annotatedModel : annotatedModels) {
            registryBuilder.addModel(annotatedModel);
        }
        return registryBuilder.build();
    }

    @Bean
    @ConditionalOnMissingBean
    public AdminPermissionService adminPermissionService() {
        LOG.debug("Registering default AdminPermissionService");

        return new AdminPermissionServiceImpl();
    }

    @Bean
    @ConditionalOnMissingBean
    public AdminModelEntitiesListService adminModelEntitiesListService(
            AdminModelRegistry modelRegistry,
            EntityManager em,
            AdminPermissionService permissionService,
            ConversionService conversionService,
            AdminModelListEntityMapper mapper
    ) {
        LOG.debug("Registering default AdminModelEntitiesListService");

        return new AdminModelEntitiesListServiceImpl(
                modelRegistry,
                em,
                permissionService,
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
            AdminPermissionService permissionService,
            ConversionService conversionService,
            MessageSource messageSource
    ) {
        LOG.debug("Registering default AdminModelFormService");

        return new AdminModelFormServiceImpl(
                modelRegistry,
                historyWriter,
                relationLinkService,
                em,
                validator,
                permissionService,
                conversionService,
                messageSource
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public AdminModelActionService adminModelActionService(
            AdminModelRegistry modelRegistry,
            AdminModelListEntityMapper mapper,
            EntityManager em,
            AdminPermissionService permissionService,
            ConversionService conversionService
    ) {
        LOG.debug("Registering default AdminModelActionService");

        return new AdminModelActionServiceImpl(modelRegistry, mapper, em, permissionService, conversionService);
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
            AdminPermissionService permissionService,
            EntityManager em
    ) {
        if (properties.isDisableHistory()) {
            return new NoOpAdminHistoryCompiler();
        } else {
            LOG.debug("Registering default AdminHistoryCompiler");

            return new AdminHistoryCompilerImpl(registry, permissionService, em);
        }
    }

    @Bean
    public AdminRelationLinkService adminRelationLinkService(
            AdminModelRegistry modelRegistry,
            AdminModelListEntityMapper mapper,
            EntityManager em,
            AdminPermissionService permissionService,
            ConversionService conversionService
    ) {
        return new AdminRelationLinkService(modelRegistry, mapper, em, permissionService, conversionService);
    }

    @Bean
    @ConditionalOnMissingBean
    public SpelExpressionContextFactory spelExpressionContextFactory() {
        LOG.debug("Registering default SpelExpressionContextFactory");

        return new SpelExpressionContextFactory();
    }

    @Bean
    public AdminModelLinkFactory adminModelLinkFactory(
            EntityManager em,
            SpelExpressionContextFactory spelExpressionContextFactory
    ) {
        return new AdminModelLinkFactory(em, spelExpressionContextFactory);
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
    public MessageHelper messageHelper(MessageSource messageSource) {
        return new MessageHelper(messageSource);
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
            AdminHistoryCompiler historyCompiler,
            AdminPermissionService permissionService
    ) {
        return new IndexController(properties, entityRegistry, historyCompiler, permissionService);
    }

    @Bean
    public ModelListController adminModelListController(
            AdminModelEntitiesListService entitiesListService,
            AdminRelationLinkService relationLinkService
    ) {
        return new ModelListController(properties, entitiesListService, relationLinkService);
    }

    @Bean
    @ConditionalOnMissingBean
    public AdminModelOptionsService adminModelOptionsService(
            AdminModelRegistry modelRegistry,
            AdminPermissionService permissionService
    ) {
        LOG.debug("Registering default AdminModelOptionsService");

        return new AdminModelOptionsServiceImpl(modelRegistry, permissionService, properties.getAutocompletePageSize());
    }

    @Bean
    public ModelFieldOptionsController adminModelFieldOptionsController(AdminModelOptionsService service) {
        return new ModelFieldOptionsController(service);
    }

    @Bean
    public ModelFormController adminModelFormController(AdminModelFormService service) {
        return new ModelFormController(properties, service);
    }

    @Bean
    public ModelActionController adminModelActionController(AdminModelActionService service) {
        return new ModelActionController(properties, service);
    }

    @Bean
    public SpringSecurityDialect springSecurityDialect() {
        return new SpringSecurityDialect();
    }
}
