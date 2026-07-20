package com.pocketcombats.admin.core.sort;

import com.pocketcombats.admin.core.AdminModelEntitiesListServiceImpl;
import com.pocketcombats.admin.core.AdminModelListEntityMapper;
import com.pocketcombats.admin.core.AdminModelListField;
import com.pocketcombats.admin.core.AdminRegisteredModel;
import com.pocketcombats.admin.core.UnknownModelException;
import com.pocketcombats.admin.core.formatter.ToStringValueFormatter;
import com.pocketcombats.admin.data.list.AdminEntityListEntry;
import com.pocketcombats.admin.data.list.AdminModelEntitiesList;
import com.pocketcombats.admin.data.list.ModelRequest;
import com.pocketcombats.admin.test.JpaTestUtils;
import com.pocketcombats.admin.test.StubPermissionService;
import com.pocketcombats.admin.test.TestCategory;
import com.pocketcombats.admin.test.TestFields;
import com.pocketcombats.admin.test.TestModels;
import com.pocketcombats.admin.test.TestPost;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.support.DefaultConversionService;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Ordering contract of the entity listing that {@link PathSortExpressionFactory} feeds into:
 * an ascending id tiebreaker is always the final order expression (making pagination
 * deterministic), and sorting by a relation attribute must not desynchronize the data
 * query from the count query.
 */
class AdminModelEntitiesListSortingTest {

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

    private AdminModelEntitiesListServiceImpl service(int pageSize) {
        AdminModelListField categoryField = new AdminModelListField(
                "category",
                "Category",
                "-",
                TestFields.reader(TestPost.class, "category"),
                new ToStringValueFormatter(),
                new PathSortExpressionFactory("category.name")
        );
        AdminRegisteredModel model = TestModels.model("post", TestPost.class)
                .label("Post")
                .entityType(em.getMetamodel().entity(TestPost.class))
                .pageSize(pageSize)
                .listFields(List.of(categoryField))
                .build();
        DefaultConversionService conversionService = new DefaultConversionService();
        return new AdminModelEntitiesListServiceImpl(
                TestModels.registry(model),
                em,
                new StubPermissionService(),
                conversionService,
                new AdminModelListEntityMapper(em, conversionService)
        );
    }

    private static void seedScrambledPosts() {
        JpaTestUtils.inTransaction(emf, tx -> {
            TestCategory alpha = new TestCategory(1L, "alpha");
            TestCategory beta = new TestCategory(2L, "beta");
            tx.persist(alpha);
            tx.persist(beta);
            // Insertion order deliberately disagrees with both id order and category-name order,
            // so a missing ORDER BY (H2 returns rows in insertion order) fails the assertions
            // instead of passing by accident
            tx.persist(new TestPost(4L, beta));
            tx.persist(new TestPost(2L, alpha));
            tx.persist(new TestPost(5L));
            tx.persist(new TestPost(3L, beta));
            tx.persist(new TestPost(1L, alpha));
        });
    }

    private static ModelRequest request(@Nullable String sort, @Nullable Integer page) {
        ModelRequest request = new ModelRequest();
        request.setSort(sort);
        request.setPage(page);
        return request;
    }

    private static List<String> ids(AdminModelEntitiesList list) {
        return list.entities().stream().map(AdminEntityListEntry::getId).toList();
    }

    @Test
    void unsortedListingIsPaginatedInDeterministicIdOrder() throws UnknownModelException {
        seedScrambledPosts();
        AdminModelEntitiesListServiceImpl service = service(2);

        assertEquals(List.of("1", "2"), ids(service.listEntities("post", request(null, 1), Map.of())));
        assertEquals(List.of("3", "4"), ids(service.listEntities("post", request(null, 2), Map.of())));
        assertEquals(List.of("5"), ids(service.listEntities("post", request(null, 3), Map.of())));
    }

    @Test
    void nonUniqueSortIsTiebrokenByAscendingIdAndKeepsNullRelationRows() throws UnknownModelException {
        seedScrambledPosts();

        AdminModelEntitiesList list = service(10).listEntities("post", request("category", null), Map.of());

        List<String> ids = ids(list);
        assertEquals(1, list.pagesCount());
        assertEquals(5, ids.size(),
                "relation sort must keep null-relation rows, staying in sync with the count query");
        // The null row's position is database-specific; the named groups must be ordered by
        // category name with ascending ids inside each group
        assertEquals(
                List.of("1", "2", "3", "4"),
                ids.stream().filter(id -> !id.equals("5")).toList(),
                "expected \"alpha\" posts before \"beta\" posts, each group in id order, got: " + ids
        );
    }

    @Test
    void descendingSortKeepsAscendingIdTiebreaker() throws UnknownModelException {
        seedScrambledPosts();

        AdminModelEntitiesList list = service(10).listEntities("post", request("-category", null), Map.of());

        List<String> ids = ids(list);
        assertEquals(5, ids.size());
        assertEquals(
                List.of("3", "4", "1", "2"),
                ids.stream().filter(id -> !id.equals("5")).toList(),
                "descending category sort must still tiebreak by ascending id, got: " + ids
        );
    }
}
