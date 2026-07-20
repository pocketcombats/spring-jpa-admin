package com.pocketcombats.admin.core.filter;

import com.pocketcombats.admin.core.EntityOptionMapper;
import com.pocketcombats.admin.core.formatter.ToStringValueFormatter;
import com.pocketcombats.admin.test.JpaTestUtils;
import com.pocketcombats.admin.test.TestCategory;
import com.pocketcombats.admin.test.TestPost;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.support.DefaultConversionService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ToOneFilterOptionsCollectorTest {

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

    @Test
    void optionsAreDistinctReferencedRelationsOrderedByLabel() {
        // "apple" must come first despite having the higher id and later insertion:
        // relation options are ordered by their formatted label, not database row order
        JpaTestUtils.inTransaction(emf, tx -> {
            TestCategory zebra = new TestCategory(1L, "zebra");
            TestCategory apple = new TestCategory(2L, "apple");
            TestCategory unreferenced = new TestCategory(3L, "mango");
            tx.persist(zebra);
            tx.persist(apple);
            tx.persist(unreferenced);
            tx.persist(new TestPost(1L, zebra));
            tx.persist(new TestPost(2L, apple));
            tx.persist(new TestPost(3L, zebra));
            tx.persist(new TestPost(4L));
        });
        ToOneFilterOptionsCollector collector = new ToOneFilterOptionsCollector(
                em,
                em.getMetamodel().entity(TestPost.class),
                em.getMetamodel().entity(TestPost.class).getAttribute("category"),
                new EntityOptionMapper(em, new DefaultConversionService(), new ToStringValueFormatter())
        );

        List<ModelFilterOption> options = collector.collectOptions(root -> em.getCriteriaBuilder().conjunction());

        assertEquals(
                List.of(
                        new ModelFilterOption("apple", "2"),
                        new ModelFilterOption("zebra", "1")
                ),
                options
        );
    }
}
