package com.pocketcombats.admin.core.search;

import com.pocketcombats.admin.test.JpaTestHarness;
import com.pocketcombats.admin.test.JpaTestUtils;
import com.pocketcombats.admin.test.TestCategory;
import com.pocketcombats.admin.test.TestComment;
import com.pocketcombats.admin.test.TestPost;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextSearchPredicateFactoryTest {

    @RegisterExtension
    static JpaTestHarness jpa = JpaTestHarness.withDefaultEntities();

    private List<Long> searchCategories(String searchQuery) {
        return JpaTestUtils.idsMatching(jpa.em(), TestCategory.class, (cb, query, root) ->
                new TextSearchPredicateFactory("name").build(cb, query, root, searchQuery).orElseThrow());
    }

    @Test
    void matchesSubstringCaseInsensitively() {
        JpaTestUtils.inTransaction(jpa.emf(), tx -> {
            tx.persist(new TestCategory(1L, "Alpha Bravo"));
            tx.persist(new TestCategory(2L, "Charlie"));
        });

        assertEquals(List.of(1L), searchCategories("ALPHA"));
    }

    @Test
    void escapesLikeWildcardsInQuery() {
        JpaTestUtils.inTransaction(jpa.emf(), tx -> {
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
            JpaTestUtils.inTransaction(jpa.emf(), tx -> tx.persist(new TestCategory(1L, "title")));

            assertEquals(List.of(1L), searchCategories("TITLE"));
        } finally {
            Locale.setDefault(originalLocale);
        }
    }

    @Test
    void dottedPathKeepsRowsWithNullRelation() {
        JpaTestUtils.inTransaction(jpa.emf(), tx -> {
            TestCategory category = new TestCategory(1L, "Alpha");
            tx.persist(category);
            tx.persist(new TestPost(1L, category));
            tx.persist(new TestPost(2L));
        });
        // Post 1 matches by category name; post 2 (no category) matches the other or-branch
        // and must not be excluded by the join the dotted path introduces
        List<Long> ids = JpaTestUtils.idsMatching(jpa.em(), TestPost.class, (cb, query, root) -> cb.or(
                new TextSearchPredicateFactory("category.name").build(cb, query, root, "alpha").orElseThrow(),
                cb.equal(root.get("id"), 2L)
        ));

        assertEquals(List.of(1L, 2L), ids);
    }

    @Test
    void multiHopDottedPathKeepsRowsWithNullIntermediateRelation() {
        JpaTestUtils.inTransaction(jpa.emf(), tx -> {
            TestCategory category = new TestCategory(1L, "Alpha");
            tx.persist(category);
            TestPost post = new TestPost(1L, category);
            tx.persist(post);
            tx.persist(new TestComment(1L, null));
            tx.persist(new TestComment(2L, post));
        });
        List<Long> ids = JpaTestUtils.idsMatching(jpa.em(), TestComment.class, (cb, query, root) -> cb.or(
                new TextSearchPredicateFactory("post.category.name").build(cb, query, root, "alpha").orElseThrow(),
                cb.equal(root.get("id"), 1L)
        ));

        assertEquals(List.of(1L, 2L), ids);
    }
}
