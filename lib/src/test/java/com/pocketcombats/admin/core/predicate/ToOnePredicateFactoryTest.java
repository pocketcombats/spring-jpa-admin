package com.pocketcombats.admin.core.predicate;

import com.pocketcombats.admin.test.JpaTestHarness;
import com.pocketcombats.admin.test.JpaTestUtils;
import com.pocketcombats.admin.test.TestCategory;
import com.pocketcombats.admin.test.TestPost;
import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.Attribute;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.convert.support.DefaultConversionService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ToOnePredicateFactoryTest {

    @RegisterExtension
    static JpaTestHarness jpa = JpaTestHarness.withDefaultEntities();

    private static void seedPosts() {
        JpaTestUtils.inTransaction(jpa.emf(), tx -> {
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
        EntityManager em = jpa.em();
        Attribute<?, ?> category = em.getMetamodel().entity(TestPost.class).getAttribute("category");
        ToOnePredicateFactory factory =
                new ToOnePredicateFactory(em, new DefaultConversionService(), category);
        return JpaTestUtils.idsMatching(em, TestPost.class,
                (cb, query, root) -> factory.createPredicate(cb, root, filterValue));
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
    @ParameterizedTest
    @ValueSource(strings = {"abc", "", "1.5", "id1"})
    void malformedFilterValueMatchesNothing(String filterValue) {
        seedPosts();

        assertEquals(List.of(), matchingPostIds(filterValue));
    }
}
