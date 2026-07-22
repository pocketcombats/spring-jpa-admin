package com.pocketcombats.admin.history;

import com.pocketcombats.admin.core.AdminRegisteredModel;
import com.pocketcombats.admin.data.history.HistoryEntry;
import com.pocketcombats.admin.test.JpaTestHarness;
import com.pocketcombats.admin.test.JpaTestUtils;
import com.pocketcombats.admin.test.StubPermissionService;
import com.pocketcombats.admin.test.TestModels;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AdminHistoryCompilerImplTest {

    private static final Instant BASE = Instant.parse("2026-01-01T00:00:00Z");

    @RegisterExtension
    static JpaTestHarness jpa = JpaTestHarness.withEntities(AdminHistoryLog.class);

    private final StubPermissionService permissions = new StubPermissionService();

    @Test
    void entriesOfModelsTheUserCannotViewAreDropped() {
        persistLog("post", "1", 1);
        persistLog("secret", "2", 2);
        persistLog("removed", "3", 3);
        permissions.deny("secret");

        List<HistoryEntry> log = compiler(model("post"), model("secret")).compileLog(10);

        assertEquals(List.of("removed", "post"), log.stream().map(HistoryEntry::model).toList());
    }

    @Test
    void entriesOfRegisteredModelsAreMappedWithModelLabel() {
        persistLog("post", "42", 1);

        List<HistoryEntry> log = compiler(model("post")).compileLog(10);

        assertEquals(List.of(new HistoryEntry("update", "post", "post label", "42", "Entity 42", "alice")), log);
    }

    @Test
    void entriesOfRemovedModelsAreKeptWithRawModelNameAsLabel() {
        persistLog("removed", "7", 1);

        List<HistoryEntry> log = compiler(model("post")).compileLog(10);

        assertEquals(List.of(new HistoryEntry("update", "removed", "removed", "7", "Entity 7", "alice")), log);
    }

    @Test
    void filteringMayYieldFewerThanRequestedEntries() {
        persistLog("post", "1", 1);
        persistLog("secret", "2", 2);
        persistLog("post", "3", 3);
        permissions.deny("secret");

        List<HistoryEntry> log = compiler(model("post"), model("secret")).compileLog(2);

        // The page is limited before filtering, so the restricted entry consumes a slot.
        assertEquals(List.of("3"), log.stream().map(HistoryEntry::id).toList());
    }

    private AdminHistoryCompilerImpl compiler(AdminRegisteredModel... models) {
        return new AdminHistoryCompilerImpl(TestModels.registry(models), permissions, jpa.em());
    }

    private void persistLog(String model, String entityId, int secondsAfterBase) {
        JpaTestUtils.inTransaction(jpa.emf(), txEm -> {
            AdminHistoryLog log = new AdminHistoryLog();
            log.setTime(BASE.plusSeconds(secondsAfterBase));
            log.setAction("update");
            log.setModel(model);
            log.setEntityId(entityId);
            log.setEntityRepresentation("Entity " + entityId);
            log.setUsername("alice");
            txEm.persist(log);
        });
    }

    private static AdminRegisteredModel model(String name) {
        return TestModels.model(name, Object.class).label(name + " label").build();
    }
}
