package com.pocketcombats.admin.history;

import com.pocketcombats.admin.core.AdminModelListEntityMapper;
import com.pocketcombats.admin.core.AdminModelListField;
import com.pocketcombats.admin.core.AdminRegisteredModel;
import com.pocketcombats.admin.test.JpaTestHarness;
import com.pocketcombats.admin.test.JpaTestUtils;
import com.pocketcombats.admin.test.TestCategory;
import com.pocketcombats.admin.test.TestModels;
import com.pocketcombats.admin.test.TestPost;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.core.convert.support.DefaultConversionService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AdminHistoryWriterImplTest {

    @RegisterExtension
    static JpaTestHarness jpa =
            JpaTestHarness.withEntities(AdminHistoryLog.class, TestCategory.class, TestPost.class);

    private final DefaultConversionService conversionService = new DefaultConversionService();

    @BeforeEach
    void seedCategory() {
        JpaTestUtils.inTransaction(jpa.emf(), em -> em.persist(new TestCategory(1L, "Category 1")));
    }

    @Test
    void representationIsTheFirstListFieldValue() {
        record(model(List.of(TestModels.categoryNameField())));

        AdminHistoryLog log = singleLog();
        assertEquals("Category 1", log.getEntityRepresentation());
        assertEquals("category", log.getModel());
        assertEquals("update", log.getAction());
        assertEquals("1", log.getEntityId());
    }

    @Test
    void representationFallsBackToEntityIdWhenModelHasNoListFields() {
        record(model(List.of()));

        assertEquals("1", singleLog().getEntityRepresentation());
    }

    private void record(AdminRegisteredModel model) {
        JpaTestUtils.inTransaction(jpa.emf(), em -> {
            AdminHistoryWriterImpl writer = new AdminHistoryWriterImpl(
                    new AdminModelListEntityMapper(em, conversionService),
                    em,
                    conversionService
            );
            writer.record(model, "update", em.find(TestCategory.class, 1L));
        });
    }

    private AdminHistoryLog singleLog() {
        try (EntityManager em = jpa.emf().createEntityManager()) {
            List<AdminHistoryLog> logs = em
                    .createQuery("SELECT l FROM AdminHistoryLog l", AdminHistoryLog.class)
                    .getResultList();
            assertEquals(1, logs.size());
            return logs.get(0);
        }
    }

    private static AdminRegisteredModel model(List<AdminModelListField> listFields) {
        return TestModels.model("category", TestCategory.class)
                .label("category label")
                .listFields(listFields)
                .build();
    }
}
