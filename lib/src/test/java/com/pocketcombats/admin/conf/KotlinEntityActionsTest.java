package com.pocketcombats.admin.conf;

import com.pocketcombats.admin.AdminAction;
import com.pocketcombats.admin.core.AdminRegisteredModel;
import com.pocketcombats.admin.core.action.AdminModelAction;
import com.pocketcombats.admin.history.NoOpAdminHistoryWriter;
import com.pocketcombats.admin.test.KotlinActionEntity;
import com.pocketcombats.admin.test.KotlinMemberActionEntity;
import com.pocketcombats.admin.test.KotlinNamedCompanionEntity;
import com.pocketcombats.admin.test.TestModels;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Entity-level {@link AdminAction @AdminAction} support for Kotlin-authored entities,
 * exercised against real kotlinc output (see {@code src/test/kotlin}).
 */
class KotlinEntityActionsTest {

    private final ActionsFactory factory = new ActionsFactory(new NoOpAdminHistoryWriter(), List.of());

    @Test
    void companionObjectActionIsDiscoveredAndRuns() {
        Map<String, AdminModelAction> actions = actions(KotlinActionEntity.class);

        assertTrue(actions.containsKey("archive"), "plain companion functions must be discovered");
        List<KotlinActionEntity> entities = List.of(new KotlinActionEntity(), new KotlinActionEntity());
        actions.get("archive").run(null, model(KotlinActionEntity.class), entities);
        assertTrue(entities.stream().allMatch(KotlinActionEntity::getArchived));
    }

    @Test
    void jvmStaticCompanionActionIsDiscoveredAndRuns() {
        Map<String, AdminModelAction> actions = actions(KotlinActionEntity.class);

        assertTrue(actions.containsKey("publish"));
        List<KotlinActionEntity> entities = List.of(new KotlinActionEntity());
        actions.get("publish").run(null, model(KotlinActionEntity.class), entities);
        assertTrue(entities.get(0).getPublished());
    }

    @Test
    void namedCompanionActionIsDiscoveredAndRuns() {
        Map<String, AdminModelAction> actions = actions(KotlinNamedCompanionEntity.class);

        assertTrue(actions.containsKey("archive"), "named companions must be discovered too");
        List<KotlinNamedCompanionEntity> entities = List.of(new KotlinNamedCompanionEntity());
        actions.get("archive").run(null, model(KotlinNamedCompanionEntity.class), entities);
        assertTrue(entities.get(0).getArchived());
    }

    @Test
    void memberFunctionActionIsRejectedWithCompanionGuidance() {
        IllegalStateException e = assertThrows(
                IllegalStateException.class,
                () -> actions(KotlinMemberActionEntity.class)
        );
        assertTrue(e.getMessage().contains("companion object"), e.getMessage());
    }

    private Map<String, AdminModelAction> actions(Class<?> entityClass) {
        return factory.createActions(TestModels.adminModelDefaults(), entityClass, null);
    }

    private static AdminRegisteredModel model(Class<?> entityClass) {
        return TestModels.model(entityClass.getSimpleName(), entityClass).build();
    }
}
