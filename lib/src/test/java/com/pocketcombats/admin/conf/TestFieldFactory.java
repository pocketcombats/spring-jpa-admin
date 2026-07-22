package com.pocketcombats.admin.conf;

import com.pocketcombats.admin.AdminModel;
import com.pocketcombats.admin.core.formatter.SpelExpressionContextFactory;
import com.pocketcombats.admin.core.search.SearchPredicateFactory;
import com.pocketcombats.admin.test.TestModels;
import jakarta.persistence.EntityManager;
import org.jspecify.annotations.Nullable;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;

import java.util.Map;

/**
 * Test-side builder for {@link FieldFactory}: model name defaults to the entity's simple class
 * name, and the wiring most tests don't care about gets production-like defaults.
 */
final class TestFieldFactory {

    private final EntityManager em;
    private final Class<?> entityClass;

    private ConversionService conversionService = new DefaultConversionService();
    private int maxPreloadedOptions = 100;
    private int maxCountedOptions = 1000;
    private Map<Class<?>, SearchPredicateFactory> searchFactoriesByEntity = Map.of();
    private AdminModel modelAnnotation = TestModels.adminModelDefaults();
    private @Nullable AdminModelBean adminModelBean;

    private TestFieldFactory(EntityManager em, Class<?> entityClass) {
        this.em = em;
        this.entityClass = entityClass;
    }

    static TestFieldFactory forEntity(EntityManager em, Class<?> entityClass) {
        return new TestFieldFactory(em, entityClass);
    }

    TestFieldFactory conversionService(ConversionService conversionService) {
        this.conversionService = conversionService;
        return this;
    }

    TestFieldFactory maxPreloadedOptions(int maxPreloadedOptions) {
        this.maxPreloadedOptions = maxPreloadedOptions;
        return this;
    }

    TestFieldFactory searchFactories(Map<Class<?>, SearchPredicateFactory> searchFactoriesByEntity) {
        this.searchFactoriesByEntity = searchFactoriesByEntity;
        return this;
    }

    TestFieldFactory modelAnnotation(AdminModel modelAnnotation) {
        this.modelAnnotation = modelAnnotation;
        return this;
    }

    TestFieldFactory adminModelBean(AdminModelBean adminModelBean) {
        this.adminModelBean = adminModelBean;
        return this;
    }

    FieldFactory build() {
        return new FieldFactory(
                em,
                conversionService,
                new SpelExpressionContextFactory(),
                maxPreloadedOptions,
                maxCountedOptions,
                searchFactoriesByEntity,
                entityClass.getSimpleName(),
                modelAnnotation,
                entityClass,
                em.getMetamodel().entity(entityClass),
                adminModelBean
        );
    }
}
