package com.pocketcombats.admin.core;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * An {@link AdminModelRegistry} that builds its metadata lazily, on first use, rather than while the
 * application context is refreshing. Reading the JPA metamodel is what forces an
 * {@link jakarta.persistence.EntityManagerFactory EntityManagerFactory} to finish initializing, so
 * deferring the build lets the admin site coexist with asynchronous JPA bootstrap
 * ({@code spring.jpa.bootstrap=async}) instead of blocking startup on the metamodel.
 * <p>
 * Once the application is ready, the registry warms up on a background executor, so the first admin
 * request does not pay the build cost, and any model-mapping error is logged shortly after startup.
 */
public class DeferredAdminModelRegistry implements AdminModelRegistry, ApplicationListener<ApplicationReadyEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(DeferredAdminModelRegistry.class);

    private final Supplier<AdminModelRegistry> factory;
    private final Executor warmUpExecutor;

    private volatile @Nullable AdminModelRegistry delegate;

    public DeferredAdminModelRegistry(Supplier<AdminModelRegistry> factory, Executor warmUpExecutor) {
        this.factory = factory;
        this.warmUpExecutor = warmUpExecutor;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        warmUpExecutor.execute(() -> {
            try {
                warmUp();
            } catch (RuntimeException e) {
                LOG.error("Failed to build admin model registry", e);
            }
        });
    }

    /**
     * Builds the backing registry if it has not been built yet. Safe to call repeatedly and from
     * multiple threads; the build runs at most once.
     */
    public void warmUp() {
        delegate();
    }

    @Override
    public AdminRegisteredModel resolve(String modelName) throws UnknownModelException {
        return delegate().resolve(modelName);
    }

    @Override
    public Optional<AdminRegisteredModel> findByEntityClass(Class<?> entityClass) {
        return delegate().findByEntityClass(entityClass);
    }

    @Override
    public List<AdminRegisteredModel> findAllByEntityClass(Class<?> entityClass) {
        return delegate().findAllByEntityClass(entityClass);
    }

    @Override
    public Map<String, List<AdminRegisteredModel>> getCategorizedModels() {
        return delegate().getCategorizedModels();
    }

    private AdminModelRegistry delegate() {
        AdminModelRegistry result = delegate;
        if (result != null) {
            return result;
        }
        synchronized (this) {
            result = delegate;
            if (result == null) {
                LOG.debug("Building admin model registry on first use");
                result = factory.get();
                delegate = result;
            }
            return result;
        }
    }
}
