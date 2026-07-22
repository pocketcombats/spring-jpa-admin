package com.pocketcombats.admin.core.filter;

import com.pocketcombats.admin.test.JpaTestHarness;
import com.pocketcombats.admin.test.JpaTestUtils;
import com.pocketcombats.admin.test.TestCategory;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.core.convert.support.DefaultConversionService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BasicFilterOptionsCollectorTest {

    @RegisterExtension
    static JpaTestHarness jpa = JpaTestHarness.withDefaultEntities();

    private EntityManager em;

    @BeforeEach
    void openEntityManager() {
        em = jpa.em();
    }

    @Test
    void optionsAreDistinctNonNullAndOrderedByValue() {
        // Insertion order deliberately disagrees with the expected (alphabetical) order,
        // so relying on undefined database order fails the assertion
        JpaTestUtils.inTransaction(jpa.emf(), tx -> {
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
