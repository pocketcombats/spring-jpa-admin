package com.pocketcombats.admin.core.search;

import com.pocketcombats.admin.test.JpaTestUtils;
import com.pocketcombats.admin.test.TestCategory;
import com.pocketcombats.admin.test.TestComment;
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

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextSearchPredicateFactoryTest {

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

    private List<Long> searchCategories(String searchQuery) {
        CriteriaQuery<TestCategory> query = cb.createQuery(TestCategory.class);
        Root<TestCategory> root = query.from(TestCategory.class);
        query.where(new TextSearchPredicateFactory("name").build(cb, root, searchQuery).orElseThrow());
        return em.createQuery(query).getResultList().stream()
                .map(TestCategory::getId)
                .sorted()
                .toList();
    }

    @Test
    void matchesSubstringCaseInsensitively() {
        JpaTestUtils.inTransaction(emf, tx -> {
            tx.persist(new TestCategory(1L, "Alpha Bravo"));
            tx.persist(new TestCategory(2L, "Charlie"));
        });

        assertEquals(List.of(1L), searchCategories("ALPHA"));
    }

    @Test
    void escapesLikeWildcardsInQuery() {
        JpaTestUtils.inTransaction(emf, tx -> {
            tx.persist(new TestCategory(1L, "100% done"));
            tx.persist(new TestCategory(2L, "1000 done"));
        });

        assertEquals(List.of(1L), searchCategories("0%"));
    }

    @Test
    void lowercasesQueryIndependentlyOfDefaultLocale() {
        // Under the Turkish default locale, "TITLE".toLowerCase() is "tıtle" (dotless i), which can
        // never match the database-lowercased column — the query must be lowercased with Locale.ROOT.
        // The stored value is already lowercase so the database's own LOWER() stays locale-neutral.
        Locale originalLocale = Locale.getDefault();
        Locale.setDefault(Locale.forLanguageTag("tr-TR"));
        try {
            JpaTestUtils.inTransaction(emf, tx -> tx.persist(new TestCategory(1L, "title")));

            assertEquals(List.of(1L), searchCategories("TITLE"));
        } finally {
            Locale.setDefault(originalLocale);
        }
    }

    @Test
    void dottedPathKeepsRowsWithNullRelation() {
        JpaTestUtils.inTransaction(emf, tx -> {
            TestCategory category = new TestCategory(1L, "Alpha");
            tx.persist(category);
            tx.persist(new TestPost(1L, category));
            tx.persist(new TestPost(2L));
        });
        CriteriaQuery<TestPost> query = cb.createQuery(TestPost.class);
        Root<TestPost> root = query.from(TestPost.class);
        Predicate search = new TextSearchPredicateFactory("category.name").build(cb, root, "alpha").orElseThrow();
        // Post 1 matches by category name; post 2 (no category) matches the other or-branch
        // and must not be excluded by the join the dotted path introduces
        query.where(cb.or(search, cb.equal(root.get("id"), 2L)));

        List<Long> ids = em.createQuery(query).getResultList().stream()
                .map(TestPost::getId)
                .sorted()
                .toList();

        assertEquals(List.of(1L, 2L), ids);
    }

    @Test
    void multiHopDottedPathKeepsRowsWithNullIntermediateRelation() {
        JpaTestUtils.inTransaction(emf, tx -> {
            TestCategory category = new TestCategory(1L, "Alpha");
            tx.persist(category);
            TestPost post = new TestPost(1L, category);
            tx.persist(post);
            tx.persist(new TestComment(1L, null));
            tx.persist(new TestComment(2L, post));
        });
        CriteriaQuery<TestComment> query = cb.createQuery(TestComment.class);
        Root<TestComment> root = query.from(TestComment.class);
        Predicate search = new TextSearchPredicateFactory("post.category.name")
                .build(cb, root, "alpha")
                .orElseThrow();
        query.where(cb.or(search, cb.equal(root.get("id"), 1L)));

        List<Long> ids = em.createQuery(query).getResultList().stream()
                .map(TestComment::getId)
                .sorted()
                .toList();

        assertEquals(List.of(1L, 2L), ids);
    }
}
