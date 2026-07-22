package com.pocketcombats.admin.core.sort;

import com.pocketcombats.admin.test.JpaTestHarness;
import com.pocketcombats.admin.test.JpaTestUtils;
import com.pocketcombats.admin.test.TestCategory;
import com.pocketcombats.admin.test.TestPost;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PathSortExpressionFactoryTest {

    @RegisterExtension
    static JpaTestHarness jpa = JpaTestHarness.withDefaultEntities();

    private List<Long> sortedPostIds(String path, boolean asc) {
        EntityManager em = jpa.em();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<TestPost> query = cb.createQuery(TestPost.class);
        Root<TestPost> root = query.from(TestPost.class);
        Expression<?> expression = new PathSortExpressionFactory(path).createExpression(root);
        query.orderBy(asc ? cb.asc(expression) : cb.desc(expression));
        return em.createQuery(query).getResultList().stream()
                .map(TestPost::getId)
                .toList();
    }

    @Test
    void sortsBySingleAttributePath() {
        JpaTestUtils.inTransaction(jpa.emf(), tx -> {
            tx.persist(new TestPost(2L));
            tx.persist(new TestPost(1L));
            tx.persist(new TestPost(3L));
        });

        assertEquals(List.of(3L, 2L, 1L), sortedPostIds("id", false));
    }

    @Test
    void sortingByRelationAttributeKeepsRowsWithNullRelation() {
        JpaTestUtils.inTransaction(jpa.emf(), tx -> {
            TestCategory beta = new TestCategory(1L, "beta");
            TestCategory alpha = new TestCategory(2L, "alpha");
            tx.persist(beta);
            tx.persist(alpha);
            tx.persist(new TestPost(1L, beta));
            tx.persist(new TestPost(2L, alpha));
            tx.persist(new TestPost(3L));
        });

        List<Long> ids = sortedPostIds("category.name", true);

        assertEquals(3, ids.size(), "sorting by a relation attribute must not drop rows with null relation");
        // The null row's position is database-specific; only the relative order of named rows is portable
        assertTrue(ids.indexOf(2L) < ids.indexOf(1L), "\"alpha\" post must sort before \"beta\" post, got: " + ids);
    }
}
