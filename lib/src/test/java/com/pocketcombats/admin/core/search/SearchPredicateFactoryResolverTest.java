package com.pocketcombats.admin.core.search;

import com.pocketcombats.admin.test.JpaTestHarness;
import com.pocketcombats.admin.test.JpaTestUtils;
import com.pocketcombats.admin.test.TestCategory;
import com.pocketcombats.admin.test.TestComment;
import com.pocketcombats.admin.test.TestPost;
import jakarta.persistence.metamodel.EntityType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.core.convert.support.DefaultConversionService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Search behavior is asserted by executing the produced predicates against a real H2 database
class SearchPredicateFactoryResolverTest {

    @RegisterExtension
    static JpaTestHarness jpa = JpaTestHarness.withDefaultEntities();

    private final SearchPredicateFactoryResolver resolver =
            new SearchPredicateFactoryResolver(new DefaultConversionService());

    @Test
    void noSearchFieldsMeansModelIsNotSearchable() {
        assertNull(resolver.resolve("TestComment", entity(TestComment.class)));
    }

    @Test
    void threeLevelSearchPathResolvesEachSegmentAgainstItsOwnEntity() {
        JpaTestUtils.inTransaction(jpa.emf(), em -> {
            TestCategory news = new TestCategory(1L, "News");
            TestCategory sports = new TestCategory(2L, "Sports");
            em.persist(news);
            em.persist(sports);
            TestPost newsPost = new TestPost(1L, news);
            TestPost sportsPost = new TestPost(2L, sports);
            em.persist(newsPost);
            em.persist(sportsPost);
            em.persist(new TestComment(1L, newsPost));
            em.persist(new TestComment(2L, sportsPost));
        });

        SearchPredicateFactory search = resolve("TestComment", TestComment.class, "post.category.name");

        assertEquals(List.of(1L), searchIds(TestComment.class, search, "news"));
    }

    @Test
    void primitiveNumericSearchFieldIsSupported() {
        JpaTestUtils.inTransaction(jpa.emf(), em -> {
            em.persist(new TestComment(1L, null, 5));
            em.persist(new TestComment(2L, null, 7));
        });

        SearchPredicateFactory search = resolve("TestComment", TestComment.class, "likes");

        assertEquals(List.of(1L), searchIds(TestComment.class, search, "5"));
    }

    @Test
    void multipleSearchFieldsMatchAsAlternatives() {
        JpaTestUtils.inTransaction(jpa.emf(), em -> {
            TestCategory news = new TestCategory(1L, "News");
            em.persist(news);
            TestPost newsPost = new TestPost(1L, news);
            em.persist(newsPost);
            em.persist(new TestComment(1L, newsPost, 5));
            em.persist(new TestComment(2L, null, 7));
        });

        SearchPredicateFactory search = resolve("TestComment", TestComment.class, "likes", "post.category.name");

        assertEquals(List.of(1L), searchIds(TestComment.class, search, "news"));
        assertEquals(List.of(2L), searchIds(TestComment.class, search, "7"));
    }

    @Test
    void searchThroughCollectionMatchesOwnerOnceDespiteMultipleMatchingElements() {
        JpaTestUtils.inTransaction(jpa.emf(), em -> {
            TestCategory tech = new TestCategory(1L, "Tech");
            TestCategory life = new TestCategory(2L, "Life");
            em.persist(tech);
            em.persist(life);
            em.persist(new TestPost(1L, tech, "Java 21"));
            em.persist(new TestPost(2L, tech, "Java 25"));
            em.persist(new TestPost(3L, life, "Cooking"));
        });

        SearchPredicateFactory search = resolve("TestCategory", TestCategory.class, "posts.title");

        // Two posts of category 1 match: a plain to-many join would return the category twice
        assertEquals(List.of(1L), searchIds(TestCategory.class, search, "java"));
    }

    @Test
    void collectionSearchComposesWithOtherSearchFieldsAsAlternatives() {
        JpaTestUtils.inTransaction(jpa.emf(), em -> {
            TestCategory news = new TestCategory(1L, "News");
            TestCategory life = new TestCategory(2L, "Life");
            TestCategory sports = new TestCategory(3L, "Sports");
            em.persist(news);
            em.persist(life);
            em.persist(sports);
            em.persist(new TestPost(1L, news, "Cooking"));
            em.persist(new TestPost(2L, life, "News roundup"));
            em.persist(new TestPost(3L, sports, "Standings"));
        });

        SearchPredicateFactory search = resolve("TestCategory", TestCategory.class, "name", "posts.title");

        // Category 1 matches by its own name, category 2 by a contained post's title;
        // a failed EXISTS branch must not drop a row the other alternative matches.
        assertEquals(List.of(1L, 2L), searchIds(TestCategory.class, search, "news"));
    }

    @Test
    void collectionSegmentsNestAndComposeWithSingularSegments() {
        JpaTestUtils.inTransaction(jpa.emf(), em -> {
            TestCategory tech = new TestCategory(1L, "Tech");
            TestCategory life = new TestCategory(2L, "Life");
            em.persist(tech);
            em.persist(life);
            em.persist(new TestPost(1L, tech, "Java 21"));
            em.persist(new TestPost(2L, tech, "Java 25"));
            em.persist(new TestPost(3L, life, "Cooking"));
        });

        SearchPredicateFactory search = resolve("TestCategory", TestCategory.class, "posts.category.posts.title");

        assertEquals(List.of(1L), searchIds(TestCategory.class, search, "java"));
    }

    @Test
    void unsupportedSearchFieldTypeErrorNamesModelAndField() {
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> resolver.resolve("TestComment", entity(TestComment.class), "post")
        );

        assertTrue(ex.getMessage().contains("TestComment"), ex.getMessage());
        assertTrue(ex.getMessage().contains("post"), ex.getMessage());
    }

    @Test
    void collectionAsTerminalSegmentIsRejected() {
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> resolver.resolve("TestCategory", entity(TestCategory.class), "posts")
        );

        assertTrue(ex.getMessage().contains("TestCategory"), ex.getMessage());
        assertTrue(ex.getMessage().contains("posts"), ex.getMessage());
    }

    @Test
    void nonAssociationSegmentErrorNamesModelAndSegment() {
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> resolver.resolve("TestCategory", entity(TestCategory.class), "name.length")
        );

        assertTrue(ex.getMessage().contains("TestCategory"), ex.getMessage());
        assertTrue(ex.getMessage().contains("\"name\""), ex.getMessage());
        assertTrue(ex.getMessage().contains("not an association"), ex.getMessage());
    }

    private SearchPredicateFactory resolve(String modelName, Class<?> entityClass, String... searchFields) {
        SearchPredicateFactory factory = resolver.resolve(modelName, entity(entityClass), searchFields);
        assertNotNull(factory, "model with search fields must be searchable");
        return factory;
    }

    private EntityType<?> entity(Class<?> entityClass) {
        return jpa.emf().getMetamodel().entity(entityClass);
    }

    private List<Long> searchIds(Class<?> entityClass, SearchPredicateFactory search, String query) {
        return JpaTestUtils.idsMatching(jpa.em(), entityClass,
                (cb, criteria, root) -> search.build(cb, criteria, root, query).orElseThrow());
    }
}
