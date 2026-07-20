package com.pocketcombats.admin.core.search;

import com.pocketcombats.admin.test.JpaTestUtils;
import com.pocketcombats.admin.test.TestCategory;
import com.pocketcombats.admin.test.TestPost;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.support.DefaultConversionService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompositeSearchPredicateFactoryTest {

    private static EntityManagerFactory emf;

    private EntityManager em;
    private CriteriaBuilder cb;

    @BeforeAll
    static void createEntityManagerFactory() {
        emf = JpaTestUtils.createEntityManagerFactory();
    }

    @AfterAll
    static void closeEntityManagerFactory() {
        emf.close();
    }

    @BeforeEach
    void openEntityManager() {
        em = emf.createEntityManager();
        cb = em.getCriteriaBuilder();
    }

    @AfterEach
    void closeEntityManagerAndWipeData() {
        em.close();
        JpaTestUtils.wipeData(emf);
    }

    @Test
    void declinesWhenThereAreNoFactories() {
        Root<TestPost> root = cb.createQuery(TestPost.class).from(TestPost.class);

        Optional<Predicate> predicate = new CompositeSearchPredicateFactory(List.of()).build(cb, root, "1");

        assertTrue(predicate.isEmpty());
    }

    @Test
    void declinesWhenEveryFactoryDeclines() {
        SearchPredicateFactory declining = (builder, r, query) -> Optional.empty();
        CompositeSearchPredicateFactory composite =
                new CompositeSearchPredicateFactory(List.of(declining, declining));
        Root<TestPost> root = cb.createQuery(TestPost.class).from(TestPost.class);

        Optional<Predicate> predicate = composite.build(cb, root, "anything");

        assertTrue(predicate.isEmpty(),
                "no contributions must yield Optional.empty(), not an always-false empty or()");
    }

    @Test
    void declinesForUnparseableQueryWhenAllSearchFieldsAreNumeric() {
        // A model with two numeric search fields: the exact shape that used to disable the
        // autocomplete id-fallback by wrapping "no contributions" into an always-false or()
        DefaultConversionService conversionService = new DefaultConversionService();
        CompositeSearchPredicateFactory composite = new CompositeSearchPredicateFactory(List.of(
                new NumberSearchPredicateFactory("id", Long.class, conversionService),
                new NumberSearchPredicateFactory("id", Long.class, conversionService)
        ));
        Root<TestPost> root = cb.createQuery(TestPost.class).from(TestPost.class);

        assertTrue(composite.build(cb, root, "not a number").isEmpty());
    }

    @Test
    void singleContributionIsReturnedAsIs() {
        Root<TestPost> root = cb.createQuery(TestPost.class).from(TestPost.class);
        Predicate contribution = cb.equal(root.get("id"), 1L);
        CompositeSearchPredicateFactory composite = new CompositeSearchPredicateFactory(List.of(
                (builder, r, query) -> Optional.empty(),
                (builder, r, query) -> Optional.of(contribution)
        ));

        assertSame(contribution, composite.build(cb, root, "1").orElseThrow());
    }

    @Test
    void multipleContributionsMatchRowsSatisfyingAnyOfThem() {
        // Post 2 has no category and matches by id; post 3 matches by category name.
        // Post 2 must not be dropped by the category join the text search introduces.
        JpaTestUtils.inTransaction(emf, tx -> {
            TestCategory category = new TestCategory(1L, "season 2");
            tx.persist(category);
            tx.persist(new TestPost(2L));
            tx.persist(new TestPost(3L, category));
        });
        CompositeSearchPredicateFactory composite = new CompositeSearchPredicateFactory(List.of(
                new NumberSearchPredicateFactory("id", Long.class, new DefaultConversionService()),
                new TextSearchPredicateFactory("category.name")
        ));
        CriteriaQuery<TestPost> query = cb.createQuery(TestPost.class);
        Root<TestPost> root = query.from(TestPost.class);
        query.where(composite.build(cb, root, "2").orElseThrow());

        List<Long> ids = em.createQuery(query).getResultList().stream()
                .map(TestPost::getId)
                .sorted()
                .toList();

        assertEquals(List.of(2L, 3L), ids);
    }
}
