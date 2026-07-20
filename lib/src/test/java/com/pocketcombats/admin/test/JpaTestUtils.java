package com.pocketcombats.admin.test;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceConfiguration;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Helpers for tests that exercise real JPA behavior against an in-memory H2 database
 * (entities {@link TestCategory}, {@link TestPost}, {@link TestComment} and {@link TestCompositeTag}).
 */
public final class JpaTestUtils {

    // Each factory gets its own database so test classes can never observe each other's data.
    private static final AtomicInteger DB_SEQUENCE = new AtomicInteger();

    private JpaTestUtils() {
    }

    /**
     * Creates the H2-backed factory; the caller is responsible for closing it.
     */
    public static EntityManagerFactory createEntityManagerFactory() {
        return createEntityManagerFactory(
                TestCategory.class,
                TestPost.class,
                TestComment.class,
                TestCompositeTag.class
        );
    }

    /**
     * Creates an H2-backed factory managing the given classes; the caller is responsible for closing it.
     */
    public static EntityManagerFactory createEntityManagerFactory(Class<?>... managedClasses) {
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

    /** Runs the given work in its own committed transaction on a dedicated {@link EntityManager}. */
    public static void inTransaction(EntityManagerFactory emf, Consumer<EntityManager> work) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            work.accept(em);
            em.getTransaction().commit();
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }

    /** Deletes all rows of the test entities, in FK-safe order. */
    public static void wipeData(EntityManagerFactory emf) {
        inTransaction(emf, em -> {
            em.createQuery("DELETE FROM TestComment").executeUpdate();
            em.createQuery("DELETE FROM TestCompositeTag").executeUpdate();
            em.createQuery("DELETE FROM TestPost").executeUpdate();
            em.createQuery("DELETE FROM TestCategory").executeUpdate();
        });
    }

    /** Persists {@link TestCategory} rows with ids {@code 1..count} named {@code "Category <id>"}. */
    public static void seedCategories(EntityManagerFactory emf, int count) {
        inTransaction(emf, em -> {
            for (long id = 1; id <= count; id++) {
                em.persist(new TestCategory(id, "Category " + id));
            }
        });
    }

    /** Persists {@link TestCompositeTag} rows {@code "ns:Tag 1".."ns:Tag <count>"}. */
    public static void seedCompositeTags(EntityManagerFactory emf, int count) {
        inTransaction(emf, em -> {
            for (int i = 1; i <= count; i++) {
                em.persist(new TestCompositeTag("ns", "Tag " + i));
            }
        });
    }
}
