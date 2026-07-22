package com.pocketcombats.admin.conf;

import com.pocketcombats.admin.AdminModel;
import com.pocketcombats.admin.core.AdminModelField;
import com.pocketcombats.admin.core.AdminModelRegistry;
import com.pocketcombats.admin.core.UnknownModelException;
import com.pocketcombats.admin.core.field.AutocompleteOptionsAccessor;
import com.pocketcombats.admin.core.formatter.SpelExpressionContextFactory;
import com.pocketcombats.admin.core.links.AdminModelLinkFactory;
import com.pocketcombats.admin.data.form.AdminSelectOption;
import com.pocketcombats.admin.history.NoOpAdminHistoryWriter;
import com.pocketcombats.admin.test.JpaTestHarness;
import com.pocketcombats.admin.test.JpaTestUtils;
import com.pocketcombats.admin.test.TestCategory;
import com.pocketcombats.admin.test.TestPost;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.convert.support.DefaultConversionService;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AdminModelRegistryBuilderTest {

    @RegisterExtension
    static JpaTestHarness jpa = JpaTestHarness.withDefaultEntities();

    @Test
    void duplicateModelNameErrorNamesBothClasses() {
        // The colliding registrations target different entities, so asserting both class names
        // proves the message names the already-registered class AND the rejected one.
        AdminModelRegistryBuilder builder = builder().addModel(FirstPostModel.class);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> builder.addModel(ImpostorCategoryModel.class)
        );

        assertTrue(ex.getMessage().contains("\"TestPost\""), ex.getMessage());
        assertTrue(ex.getMessage().contains(TestPost.class.getName()), ex.getMessage());
        assertTrue(ex.getMessage().contains(TestCategory.class.getName()), ex.getMessage());
        assertTrue(ex.getMessage().contains("simple class name"), ex.getMessage());
    }

    @Test
    void nameAttributeOverridesRegistryKey() throws Exception {
        AdminModelRegistry registry = builder()
                .addModel(FirstPostModel.class)
                .addModel(AliasedPostModel.class)
                .build();

        assertEquals("TestPost", registry.resolve("TestPost").modelName());
        assertEquals("AliasedPost", registry.resolve("AliasedPost").modelName());
    }

    @Test
    void autocompleteEligibilityIsIndependentOfTargetRegistrationOrder() throws Exception {
        JpaTestUtils.seedCategories(jpa.emf(), 3);

        AdminModelRegistry registry = builder(2)
                .addModel(OrderIndependentPostModel.class)
                .addModel(OrderIndependentCategoryModel.class)
                .build();

        Map<String, ?> attributes = categoryField(registry).valueAccessor().getModelAttributes(new TestPost(1L));

        assertEquals(Boolean.TRUE, attributes.get("_autocomplete"));
    }

    @Test
    void searchableTargetBakesAWorkingSearchFactoryIntoTheToOneAccessor() throws UnknownModelException {
        JpaTestUtils.seedCategories(jpa.emf(), 3);

        AdminModelRegistry registry = builder(2)
                .addModel(OrderIndependentPostModel.class)
                .addModel(PlainCategoryModel.class)
                .addModel(SearchableAuxCategoryModel.class)
                .build();

        AutocompleteOptionsAccessor accessor =
                (AutocompleteOptionsAccessor) categoryField(registry).valueAccessor();

        assertTrue(accessor.autocompleteSupported(), "a searchable registration must make the field serve options");
        assertEquals(
                List.of(new AdminSelectOption("id2", "Category 2")),
                accessor.collectOptions("Category 2", 1, 20).results(),
                "the searchable registration's factory must be baked in and applied by collectOptions"
        );
    }

    private static AdminModelField categoryField(AdminModelRegistry registry) throws UnknownModelException {
        return registry.resolve("TestPost").fieldsets().stream()
                .flatMap(fieldset -> fieldset.fields().stream())
                .filter(field -> field.name().equals("category"))
                .findFirst()
                .orElseThrow();
    }

    @Test
    void nonPositivePageSizeIsRejected() {
        AdminConfigurationException ex = assertThrows(
                AdminConfigurationException.class,
                () -> builder().addModel(ZeroPageSizeModel.class)
        );

        assertTrue(ex.getMessage().contains("TestPost"), ex.getMessage());
    }

    private AdminModelRegistryBuilder builder() {
        return builder(100);
    }

    private AdminModelRegistryBuilder builder(int maxPreloadedOptions) {
        EntityManager em = jpa.em();
        SpelExpressionContextFactory spelContextFactory = new SpelExpressionContextFactory();
        return new AdminModelRegistryBuilder(
                em,
                new DefaultListableBeanFactory(),
                new DefaultConversionService(),
                spelContextFactory,
                new AdminModelLinkFactory(em, spelContextFactory),
                new ActionsFactory(new NoOpAdminHistoryWriter(), List.of()),
                maxPreloadedOptions,
                1000
        );
    }

    @AdminModel(entity = TestPost.class)
    static class FirstPostModel {
    }

    // Claims TestPost's default model name while targeting a different entity.
    @AdminModel(entity = TestCategory.class, name = "TestPost")
    static class ImpostorCategoryModel {
    }

    @AdminModel(entity = TestPost.class, name = "AliasedPost")
    static class AliasedPostModel {
    }

    @AdminModel(entity = TestPost.class, pageSize = 0)
    static class ZeroPageSizeModel {
    }

    @AdminModel(entity = TestPost.class)
    static class OrderIndependentPostModel {
    }

    @AdminModel(entity = TestCategory.class, searchFields = "name")
    static class OrderIndependentCategoryModel {
    }

    // Default-named ("TestCategory") registration with no search configured: the primary registration
    // of the entity, deliberately not searchable.
    @AdminModel(entity = TestCategory.class)
    static class PlainCategoryModel {
    }

    // A secondary (custom-named) registration that *is* searchable.
    @AdminModel(entity = TestCategory.class, name = "aux", searchFields = "name")
    static class SearchableAuxCategoryModel {
    }
}
