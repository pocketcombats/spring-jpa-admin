package com.pocketcombats.admin.test;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceConfiguration;
import jakarta.persistence.metamodel.EntityType;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * JUnit extension owning the H2-backed JPA lifecycle: one database per test class, a data wipe
 * before every test (so a crashed test can't poison the next), and a fresh {@link EntityManager}
 * per test.
 * <p>
 * Register as a static field:
 * <pre>{@code @RegisterExtension static JpaTestHarness jpa = JpaTestHarness.withDefaultEntities();}</pre>
 */
public final class JpaTestHarness
        implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback, AfterEachCallback {

    private static final Class<?>[] DEFAULT_ENTITIES = {
            TestCategory.class,
            TestPost.class,
            TestComment.class,
            TestCompositeTag.class
    };

    // Each factory gets its own database, so test classes can never observe each other's data
    private static final AtomicInteger DB_SEQUENCE = new AtomicInteger();

    private final Class<?>[] managedClasses;

    private EntityManagerFactory emf;
    private EntityManager em;

    private JpaTestHarness(Class<?>[] managedClasses) {
        this.managedClasses = managedClasses;
    }

    public static JpaTestHarness withDefaultEntities() {
        return new JpaTestHarness(DEFAULT_ENTITIES.clone());
    }

    public static JpaTestHarness withEntities(Class<?>... managedClasses) {
        return new JpaTestHarness(managedClasses.clone());
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        emf = createEntityManagerFactory();
    }

    @Override
    public void afterAll(ExtensionContext context) {
        emf.close();
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        wipeData();
        em = emf.createEntityManager();
    }

    @Override
    public void afterEach(ExtensionContext context) {
        em.close();
    }

    public EntityManagerFactory emf() {
        return emf;
    }

    public EntityManager em() {
        return em;
    }

    private EntityManagerFactory createEntityManagerFactory() {
        String name = "spring-jpa-admin-test-" + DB_SEQUENCE.incrementAndGet();
        PersistenceConfiguration configuration = new PersistenceConfiguration(name);
        for (Class<?> managedClass : managedClasses) {
            configuration.managedClass(managedClass);
        }
        return configuration
                .property(PersistenceConfiguration.JDBC_DRIVER, "org.h2.Driver")
                .property(PersistenceConfiguration.JDBC_URL, "jdbc:h2:mem:" + name + ";DB_CLOSE_DELAY=-1")
                .property(PersistenceConfiguration.JDBC_USER, "sa")
                .property(PersistenceConfiguration.JDBC_PASSWORD, "")
                .property(PersistenceConfiguration.SCHEMAGEN_DATABASE_ACTION, "drop-and-create")
                .createEntityManagerFactory();
    }

    private void wipeData() {
        JpaTestUtils.inTransaction(emf, tx -> {
            tx.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate();
            try {
                for (EntityType<?> entity : emf.getMetamodel().getEntities()) {
                    tx.createQuery("DELETE FROM " + entity.getName()).executeUpdate();
                }
            } finally {
                tx.createNativeQuery("SET REFERENTIAL_INTEGRITY TRUE").executeUpdate();
            }
        });
    }
}
