package com.pocketcombats.admin.conf;

import com.pocketcombats.admin.AdminAction;
import com.pocketcombats.admin.AdminModel;
import com.pocketcombats.admin.core.AdminRegisteredModel;
import com.pocketcombats.admin.core.action.AdminModelAction;
import com.pocketcombats.admin.history.NoOpAdminHistoryWriter;
import com.pocketcombats.admin.test.KotlinActionEntity;
import com.pocketcombats.admin.test.TestModels;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ActionsFactoryTest {

    private final ActionsFactory factory = new ActionsFactory(new NoOpAdminHistoryWriter(), List.of());

    @Test
    void staticEntityActionIsDiscoveredAndRuns() {
        Map<String, AdminModelAction> actions = actions(ArchivablePost.class);

        assertTrue(actions.containsKey("archive"));
        List<ArchivablePost> posts = List.of(new ArchivablePost(), new ArchivablePost());
        actions.get("archive").run(null, model(ArchivablePost.class), posts);
        assertTrue(posts.stream().allMatch(post -> post.archived));
    }

    @Test
    void instanceEntityActionIsRejectedWithCompanionGuidance() {
        IllegalStateException e = assertThrows(
                IllegalStateException.class,
                () -> actions(BrokenActionPost.class)
        );
        assertTrue(e.getMessage().contains("companion object"), e.getMessage());
        assertTrue(e.getMessage().contains("BrokenActionPost#archive"), e.getMessage());
    }

    // The companion scan is gated on the kotlin.Metadata annotation, so the exact bytecode shape
    // kotlinc emits must NOT be recognized on a Java class: a same-named nested holder constant is
    // a common Java singleton idiom, and treating it as a companion would register its methods as
    // actions (bypassing the "entity actions MUST be static" rule) and force access to a field the
    // application never meant to expose.
    @Test
    void companionShapedJavaSingletonIsNotTreatedAsACompanion() {
        Map<String, AdminModelAction> actions = actions(CompanionShapedPost.class);

        assertFalse(actions.containsKey("archive"), "holder methods must not be registered as actions");
    }

    @Test
    void adminModelActionOverridesCompanionActionOfTheSameName() {
        Map<String, AdminModelAction> actions = factory.createActions(
                TestModels.adminModelDefaults(),
                KotlinActionEntity.class,
                new AdminModelBean(KotlinPostAdminModel.class, new KotlinPostAdminModel())
        );

        List<KotlinActionEntity> posts = List.of(new KotlinActionEntity());
        actions.get("archive").run(null, model(KotlinActionEntity.class), posts);
        assertTrue(posts.get(0).getPublished(), "admin model actions must have the highest precedence");
        assertFalse(posts.get(0).getArchived(), "the companion's same-named action must not run");
    }

    @Test
    void entityActionOverridesDefaultActionOfTheSameName() {
        Map<String, AdminModelAction> actions = factoryWithDefaultDelete().createActions(
                TestModels.adminModelDefaults(),
                DeleteOverridingPost.class,
                null
        );

        // Entity's own static delete marks the post, while the default StubDeleteAction is a no-op,
        // so a set flag proves the override won.
        List<DeleteOverridingPost> posts = List.of(new DeleteOverridingPost());
        actions.get("delete").run(null, model(DeleteOverridingPost.class), posts);
        assertTrue(posts.get(0).deleted, "the entity's own delete action must run, not the default");
    }

    @Test
    void disabledActionsAreRemoved() {
        Map<String, AdminModelAction> actions = factoryWithDefaultDelete().createActions(
                NoDeleteModel.class.getAnnotation(AdminModel.class),
                ArchivablePost.class,
                null
        );

        assertFalse(actions.containsKey("delete"));
        assertTrue(actions.containsKey("archive"));
    }

    private Map<String, AdminModelAction> actions(Class<?> entityClass) {
        return factory.createActions(TestModels.adminModelDefaults(), entityClass, null);
    }

    private static ActionsFactory factoryWithDefaultDelete() {
        return new ActionsFactory(new NoOpAdminHistoryWriter(), List.of(new StubDeleteAction()));
    }

    private static AdminRegisteredModel model(Class<?> entityClass) {
        return TestModels.model(entityClass.getSimpleName(), entityClass).build();
    }

    @AdminModel(disableActions = "delete")
    private static final class NoDeleteModel {
    }

    public static class ArchivablePost {

        boolean archived;

        @AdminAction
        public static void archive(List<ArchivablePost> posts) {
            for (ArchivablePost post : posts) {
                post.archived = true;
            }
        }
    }

    public static class BrokenActionPost {

        @AdminAction
        public void archive(List<BrokenActionPost> posts) {
        }
    }

    /**
     * A Java class with the exact shape kotlinc emits for {@code companion object}: a legitimate
     * nested-singleton idiom that must never be scanned for actions.
     */
    public static class CompanionShapedPost {

        public static final Companion Companion = new Companion();

        public static final class Companion {

            @AdminAction
            public void archive(List<CompanionShapedPost> posts) {
            }
        }
    }

    public static class KotlinPostAdminModel {

        @AdminAction
        public void archive(List<KotlinActionEntity> posts) {
            for (KotlinActionEntity post : posts) {
                post.setPublished(true);
            }
        }
    }

    public static class DeleteOverridingPost {

        boolean deleted;

        @AdminAction
        public static void delete(List<DeleteOverridingPost> posts) {
            for (DeleteOverridingPost post : posts) {
                post.deleted = true;
            }
        }
    }

    private static final class StubDeleteAction implements AdminModelAction {

        @Override
        public String getId() {
            return "delete";
        }

        @Override
        public String getLabel() {
            return "Delete";
        }

        @Override
        public String getDescription() {
            return "";
        }

        @Override
        public void run(EntityManager em, AdminRegisteredModel model, List<?> entities) {
        }
    }
}
