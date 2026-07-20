package com.pocketcombats.admin.core.filter;

import com.pocketcombats.admin.test.JpaTestUtils;
import com.pocketcombats.admin.test.TestCategory;
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

class BasicFilterOptionsCollectorTest {

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
    void optionsAreDistinctNonNullAndOrderedByValue() {
        // Insertion order deliberately disagrees with the expected (alphabetical) order,
        // so relying on undefined database order fails the assertion
        JpaTestUtils.inTransaction(emf, tx -> {
            tx.persist(new TestCategory(1L, "cherry"));
            tx.persist(new TestCategory(2L, "apple"));
            tx.persist(new TestCategory(3L, "banana"));
            tx.persist(new TestCategory(4L, null));
            tx.persist(new TestCategory(5L, "apple"));
        });
        BasicFilterOptionsCollector collector = new BasicFilterOptionsCollector(
                em,
                new DefaultConversionService(),
                em.getMetamodel().entity(TestCategory.class),
                em.getMetamodel().entity(TestCategory.class).getAttribute("name")
        );

        List<ModelFilterOption> options = collector.collectOptions(root -> em.getCriteriaBuilder().conjunction());

        assertEquals(
                List.of(
                        new ModelFilterOption("apple", "apple"),
                        new ModelFilterOption("banana", "banana"),
                        new ModelFilterOption("cherry", "cherry")
                ),
                options
        );
    }
}
