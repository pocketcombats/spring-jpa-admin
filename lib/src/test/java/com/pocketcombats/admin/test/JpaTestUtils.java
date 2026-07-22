package com.pocketcombats.admin.test;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.criteria.AbstractQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.util.List;
import java.util.function.Consumer;

/**
 * Helpers for tests that exercise real JPA behavior against the in-memory H2 database set up by
 * {@link JpaTestHarness} (entities {@link TestCategory}, {@link TestPost}, {@link TestComment}
 * and {@link TestCompositeTag}).
 */
public final class JpaTestUtils {

    private JpaTestUtils() {
    }

    /**
     * Runs the given work in its own committed transaction on a dedicated {@link EntityManager}.
     */
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

    /**
     * Builds the predicate under test against the query being executed.
     */
    @FunctionalInterface
    public interface PredicateSource {
        Predicate create(CriteriaBuilder cb, AbstractQuery<?> query, Root<?> root);
    }

    /**
     * Ids of {@code entityClass} rows (Long-id entities only) matching the predicate, in ascending
     * id order and including duplicates — a row-multiplying predicate shows up as a repeated id.
     */
    public static List<Long> idsMatching(EntityManager em, Class<?> entityClass, PredicateSource predicate) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> criteria = cb.createQuery(Long.class);
        Root<?> root = criteria.from(entityClass);
        criteria.select(root.get("id"));
        criteria.where(predicate.create(cb, criteria, root));
        criteria.orderBy(cb.asc(root.get("id")));
        return em.createQuery(criteria).getResultList();
    }

    /**
     * Persists {@link TestCategory} rows with ids {@code 1..count} named {@code "Category <id>"}.
     */
    public static void seedCategories(EntityManagerFactory emf, int count) {
        inTransaction(emf, em -> {
            for (long id = 1; id <= count; id++) {
                em.persist(new TestCategory(id, "Category " + id));
            }
        });
    }

    /**
     * Persists {@link TestCompositeTag} rows {@code "ns:Tag 1".."ns:Tag <count>"}.
     */
    public static void seedCompositeTags(EntityManagerFactory emf, int count) {
        inTransaction(emf, em -> {
            for (int i = 1; i <= count; i++) {
                em.persist(new TestCompositeTag("ns", "Tag " + i));
            }
        });
    }
}
