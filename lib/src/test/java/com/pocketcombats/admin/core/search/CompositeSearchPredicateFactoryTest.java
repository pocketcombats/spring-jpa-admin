package com.pocketcombats.admin.core.search;

import com.pocketcombats.admin.test.JpaTestHarness;
import com.pocketcombats.admin.test.JpaTestUtils;
import com.pocketcombats.admin.test.TestCategory;
import com.pocketcombats.admin.test.TestPost;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.core.convert.support.DefaultConversionService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CompositeSearchPredicateFactoryTest {

    @RegisterExtension
    static JpaTestHarness jpa = JpaTestHarness.withDefaultEntities();

    private CriteriaBuilder cb;

    @BeforeEach
    void createCriteriaBuilder() {
        cb = jpa.em().getCriteriaBuilder();
    }

    @Test
    void declinesWhenThereAreNoFactories() {
        CriteriaQuery<TestPost> criteria = cb.createQuery(TestPost.class);
        Root<TestPost> root = criteria.from(TestPost.class);

        Optional<Predicate> predicate = new CompositeSearchPredicateFactory(List.of()).build(cb, criteria, root, "1");

        assertTrue(predicate.isEmpty());
    }

    @Test
    void declinesWhenEveryFactoryDeclines() {
        SearchPredicateFactory declining = (builder, q, from, query) -> Optional.empty();
        CompositeSearchPredicateFactory composite =
                new CompositeSearchPredicateFactory(List.of(declining, declining));
        CriteriaQuery<TestPost> criteria = cb.createQuery(TestPost.class);
        Root<TestPost> root = criteria.from(TestPost.class);

        Optional<Predicate> predicate = composite.build(cb, criteria, root, "anything");

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
        CriteriaQuery<TestPost> criteria = cb.createQuery(TestPost.class);
        Root<TestPost> root = criteria.from(TestPost.class);

        assertTrue(composite.build(cb, criteria, root, "not a number").isEmpty());
    }

    @Test
    void singleContributionIsReturnedAsIs() {
        CriteriaQuery<TestPost> criteria = cb.createQuery(TestPost.class);
        Root<TestPost> root = criteria.from(TestPost.class);
        Predicate contribution = cb.equal(root.get("id"), 1L);
        CompositeSearchPredicateFactory composite = new CompositeSearchPredicateFactory(List.of(
                (builder, q, from, query) -> Optional.empty(),
                (builder, q, from, query) -> Optional.of(contribution)
        ));

        assertSame(contribution, composite.build(cb, criteria, root, "1").orElseThrow());
    }

    @Test
    void multipleContributionsMatchRowsSatisfyingAnyOfThem() {
        // Post 2 has no category and matches by id; post 3 matches by category name.
        // Post 2 must not be dropped by the category join the text search introduces.
        JpaTestUtils.inTransaction(jpa.emf(), tx -> {
            TestCategory category = new TestCategory(1L, "season 2");
            tx.persist(category);
            tx.persist(new TestPost(2L));
            tx.persist(new TestPost(3L, category));
        });
        CompositeSearchPredicateFactory composite = new CompositeSearchPredicateFactory(List.of(
                new NumberSearchPredicateFactory("id", Long.class, new DefaultConversionService()),
                new TextSearchPredicateFactory("category.name")
        ));
        List<Long> ids = JpaTestUtils.idsMatching(jpa.em(), TestPost.class,
                (builder, query, root) -> composite.build(builder, query, root, "2").orElseThrow());

        assertEquals(List.of(2L, 3L), ids);
    }
}
