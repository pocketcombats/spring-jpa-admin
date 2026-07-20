package com.pocketcombats.admin.core.predicate;

import com.pocketcombats.admin.test.JpaTestUtils;
import com.pocketcombats.admin.test.TestComment;
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

class BasicPredicateFactoryTest {

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

    private static void seedComments() {
        JpaTestUtils.inTransaction(emf, tx -> {
            tx.persist(new TestComment(1L, null, 3));
            tx.persist(new TestComment(2L, null, 5));
        });
    }

    private List<Long> matchingCommentIds(@Nullable Object filterValue) {
        return matchingCommentIds("likes", filterValue);
    }

    private List<Long> matchingCommentIds(String attributeName, @Nullable Object filterValue) {
        Attribute<?, ?> attribute = em.getMetamodel().entity(TestComment.class).getAttribute(attributeName);
        BasicPredicateFactory factory = new BasicPredicateFactory(new DefaultConversionService(), attribute);
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<TestComment> query = cb.createQuery(TestComment.class);
        Root<TestComment> root = query.from(TestComment.class);
        query.where(factory.createPredicate(cb, root, filterValue));
        return em.createQuery(query).getResultList().stream()
                .map(TestComment::getId)
                .sorted()
                .toList();
    }

    @Test
    void convertsStringFilterValueToAttributeType() {
        seedComments();

        assertEquals(List.of(2L), matchingCommentIds("5"));
    }

    @Test
    void acceptsValueAlreadyOfAttributeType() {
        seedComments();

        assertEquals(List.of(1L), matchingCommentIds(3));
    }

    @Test
    void nullFilterValueMatchesRowsWithNullAttribute() {
        JpaTestUtils.inTransaction(emf, tx -> {
            tx.persist(new TestComment(1L, null, 3));
            TestComment flagged = new TestComment(2L, null, 5);
            flagged.setModerationNote("spam");
            tx.persist(flagged);
        });

        assertEquals(List.of(1L), matchingCommentIds("moderationNote", null));
    }

    // Filter values arrive as raw URL parameters; a malformed one must match nothing
    // instead of escaping as a conversion exception. The empty string converts to null.
    @ParameterizedTest
    @ValueSource(strings = {"abc", "", "5.5", "5abc"})
    void malformedFilterValueMatchesNothing(String filterValue) {
        seedComments();

        assertEquals(List.of(), matchingCommentIds(filterValue));
    }
}
