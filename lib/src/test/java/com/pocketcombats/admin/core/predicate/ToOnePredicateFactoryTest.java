package com.pocketcombats.admin.core.predicate;

import com.pocketcombats.admin.test.JpaTestUtils;
import com.pocketcombats.admin.test.TestCategory;
import com.pocketcombats.admin.test.TestPost;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.Attribute;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.convert.support.DefaultConversionService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ToOnePredicateFactoryTest {

    private static EntityManagerFactory emf;

    private EntityManager em;

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
    }

    @AfterEach
    void closeEntityManagerAndWipeData() {
        em.close();
        JpaTestUtils.wipeData(emf);
    }

    private static void seedPosts() {
        JpaTestUtils.inTransaction(emf, tx -> {
            TestCategory alpha = new TestCategory(1L, "alpha");
            TestCategory beta = new TestCategory(2L, "beta");
            tx.persist(alpha);
            tx.persist(beta);
            tx.persist(new TestPost(1L, alpha));
            tx.persist(new TestPost(2L, beta));
            tx.persist(new TestPost(3L));
        });
    }

    private List<Long> matchingPostIds(@Nullable Object filterValue) {
        Attribute<?, ?> category = em.getMetamodel().entity(TestPost.class).getAttribute("category");
        ToOnePredicateFactory factory =
                new ToOnePredicateFactory(em, new DefaultConversionService(), category);
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<TestPost> query = cb.createQuery(TestPost.class);
        Root<TestPost> root = query.from(TestPost.class);
        query.where(factory.createPredicate(cb, root, filterValue));
        return em.createQuery(query).getResultList().stream()
                .map(TestPost::getId)
                .sorted()
                .toList();
    }

    @Test
    void convertsStringFilterValueToRelationId() {
        seedPosts();

        assertEquals(List.of(1L), matchingPostIds("1"));
    }

    @Test
    void nullFilterValueMatchesRowsWithoutRelation() {
        seedPosts();

        assertEquals(List.of(3L), matchingPostIds(null));
    }

    // Filter values arrive as raw URL parameters; a value that cannot be converted to the
    // relation's id type must match nothing instead of escaping as a conversion exception.
    // The empty string converts to null (and used to reach em.getReference(type, null)).
    @ParameterizedTest
    @ValueSource(strings = {"abc", "", "1.5", "id1"})
    void malformedFilterValueMatchesNothing(String filterValue) {
        seedPosts();

        assertEquals(List.of(), matchingPostIds(filterValue));
    }
}
