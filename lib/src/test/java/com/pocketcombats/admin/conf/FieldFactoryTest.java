package com.pocketcombats.admin.conf;

import com.pocketcombats.admin.AdminField;
import com.pocketcombats.admin.AdminFieldOverride;
import com.pocketcombats.admin.AdminModel;
import com.pocketcombats.admin.core.AdminModelField;
import com.pocketcombats.admin.core.field.AdminFormFieldSingularValueAccessor;
import com.pocketcombats.admin.core.search.SearchPredicateFactory;
import com.pocketcombats.admin.test.JpaTestHarness;
import com.pocketcombats.admin.test.JpaTestUtils;
import com.pocketcombats.admin.test.TestCategory;
import com.pocketcombats.admin.test.TestComment;
import com.pocketcombats.admin.test.TestCompositeTagId;
import com.pocketcombats.admin.test.TestPost;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

// The max-preloaded-options rules are asserted through the widget mode of the accessor that
// FieldFactory actually builds against the real metamodel, so the annotation -> accessor wiring is
// covered, not just the resolution arithmetic.
class FieldFactoryTest {

    // A no-op search factory: these tests only care that a target entity is registered as searchable
    // (map membership is what drives autocomplete capability), never how it actually searches.
    private static final SearchPredicateFactory SEARCHABLE = (cb, criteria, from, query) -> Optional.empty();

    @RegisterExtension
    static JpaTestHarness jpa = JpaTestHarness.withDefaultEntities();

    private EntityManager em;

    @BeforeEach
    void openEntityManager() {
        em = jpa.em();
        JpaTestUtils.seedCategories(jpa.emf(), 3);
    }

    // 3 categories are seeded, so a threshold below 3 switches the to-one field to autocomplete, and a
    // threshold of 3+ keeps it a preloaded select, unless a field override changes the resolved value.
    @ParameterizedTest(name = "{3}")
    @MethodSource("thresholdResolutionCases")
    void toOneWidgetModeFollowsResolvedThreshold(
            Class<?> modelHolder, int globalThreshold, boolean expectedAutocomplete, String description) {
        assertEquals(expectedAutocomplete, isAutocomplete(formField(modelHolder, globalThreshold)), description);
    }

    static Stream<Arguments> thresholdResolutionCases() {
        return Stream.of(
                arguments(PlainModel.class, 2, true, "no field config: 3 rows exceed the global threshold of 2"),
                arguments(PlainModel.class, 100, false, "no field config: 3 rows fit within the global threshold of 100"),
                arguments(UnsetOverrideModel.class, 2, true, "an unset field override inherits the global threshold"),
                arguments(OverridingModel.class, 100, true, "a field override of 2 beats the global 100"),
                arguments(DisablingModel.class, 2, false, "a negative field override disables autocomplete regardless of global")
        );
    }

    @Test
    void customTemplateFieldStaysPreloadedAboveThreshold() {
        AdminModelField field = formField(CustomTemplateModel.class, 2);

        assertEquals("admin/widget/custom-toone", field.template());
        assertFalse(isAutocomplete(field), "custom-template fields must never switch to autocomplete");
        assertTrue(
                field.valueAccessor().getModelAttributes(new TestPost(1L)).containsKey("_options"),
                "custom templates rely on the preloaded _options contract"
        );
    }

    @Test
    void unregisteredTargetStaysPreloadedAboveThreshold() {
        // TestCategory is deliberately excluded from the searchable-targets set here, simulating a
        // to-one field whose target isn't (yet, or ever) a registered, searchable admin model.
        AdminModelField field = formField(PlainModel.class, 2, TestPost.class, "category", Map.of());

        assertFalse(isAutocomplete(field), "an unregistered or non-searchable target is never autocomplete-eligible");
        assertTrue(field.valueAccessor().getModelAttributes(new TestPost(1L)).containsKey("_options"));
    }

    @Test
    void compositeIdTargetStaysPreloadedAboveThreshold() {
        JpaTestUtils.seedCompositeTags(jpa.emf(), 3);

        AdminModelField field = formField(PlainModel.class, 2, TestComment.class, "tag");

        assertFalse(isAutocomplete(field, new TestComment(1L, null)), "composite-id targets are never autocomplete-eligible");
        assertTrue(field.valueAccessor().getModelAttributes(new TestComment(1L, null)).containsKey("_options"));
    }

    @Test
    void setterOnlyBeanPropertyFallsBackToFieldReader() {
        AdminModelField field = formField(PlainModel.class, 100, TestComment.class, "moderationNote");

        assertEquals(String.class, field.valueAccessor().getReaderJavaType());
        assertTrue(field.valueAccessor().isWritable());

        TestComment comment = new TestComment(1L, null);
        comment.setModerationNote("needs review");
        assertEquals("needs review", field.valueAccessor().readValue(comment));
    }

    @Test
    void adminModelTwoArgSetterIsDiscoveredAsWriter() {
        NoteAdminModel adminModel = new NoteAdminModel();
        // "note" is a custom (admin-model-level) String field
        AdminModelField field = adminModelFormField(adminModel, "note");

        assertTrue(field.valueAccessor().isWritable());
        assertEquals(String.class, field.valueAccessor().getWriterJavaType());

        TestPost post = new TestPost(1L);
        BindingResult bindingResult = new BeanPropertyBindingResult(post, "post");
        ((AdminFormFieldSingularValueAccessor) field.valueAccessor()).setValue(post, "hello", bindingResult);

        assertFalse(bindingResult.hasErrors());
        assertEquals("hello", adminModel.getNote(post));
    }

    @Test
    void readerOnlyComputedFieldIsReadOnly() {
        AdminModelField field = adminModelFormField(new NoteAdminModel(), "preview");

        assertFalse(field.valueAccessor().isWritable(), "computed fields without a setter must be read-only");
        assertEquals("Preview", field.valueAccessor().readValue(new TestPost(1L)));
    }

    private AdminModelField adminModelFormField(NoteAdminModel adminModel, String fieldName) {
        return TestFieldFactory.forEntity(em, TestPost.class)
                .conversionService(conversionService())
                .modelAnnotation(PlainModel.class.getAnnotation(AdminModel.class))
                .adminModelBean(new AdminModelBean(NoteAdminModel.class, adminModel))
                .build()
                .constructFormField(fieldName);
    }

    // "category" is the field used throughout; TestCategory is always treated as a registered,
    // searchable admin model so these tests isolate the threshold/template/id behavior under test.
    private AdminModelField formField(Class<?> modelHolder, int globalMaxPreloadedOptions) {
        return formField(modelHolder, globalMaxPreloadedOptions, TestPost.class, "category", Map.of(TestCategory.class, SEARCHABLE));
    }

    private AdminModelField formField(
            Class<?> modelHolder,
            int globalMaxPreloadedOptions,
            Class<?> entityClass,
            String fieldName
    ) {
        return formField(modelHolder, globalMaxPreloadedOptions, entityClass, fieldName, Map.of());
    }

    private AdminModelField formField(
            Class<?> modelHolder,
            int globalMaxPreloadedOptions,
            Class<?> entityClass,
            String fieldName,
            Map<Class<?>, SearchPredicateFactory> searchFactoriesByEntity
    ) {
        return TestFieldFactory.forEntity(em, entityClass)
                .conversionService(conversionService())
                .maxPreloadedOptions(globalMaxPreloadedOptions)
                .searchFactories(searchFactoriesByEntity)
                .modelAnnotation(modelHolder.getAnnotation(AdminModel.class))
                .build()
                .constructFormField(fieldName);
    }

    // Rendering options for a composite-id target requires an id-to-String converter
    private static DefaultConversionService conversionService() {
        DefaultConversionService conversionService = new DefaultConversionService();
        conversionService.addConverter(TestCompositeTagId.class, String.class, TestCompositeTagId::toString);
        return conversionService;
    }

    // Most fields under test belong to TestPost; fields of other entities pass their own instance
    private static boolean isAutocomplete(AdminModelField field) {
        return isAutocomplete(field, new TestPost(1L));
    }

    private static boolean isAutocomplete(AdminModelField field, Object instance) {
        Map<String, ?> attributes = field.valueAccessor().getModelAttributes(instance);
        return Boolean.TRUE.equals(attributes.get("_autocomplete"));
    }

    @AdminModel
    private static final class PlainModel {
    }

    @AdminModel(fieldOverrides = @AdminFieldOverride(name = "category", field = @AdminField))
    private static final class UnsetOverrideModel {
    }

    @AdminModel(fieldOverrides = @AdminFieldOverride(name = "category", field = @AdminField(maxPreloadedOptions = 2)))
    private static final class OverridingModel {
    }

    @AdminModel(fieldOverrides = @AdminFieldOverride(name = "category", field = @AdminField(maxPreloadedOptions = -1)))
    private static final class DisablingModel {
    }

    @AdminModel(fieldOverrides = @AdminFieldOverride(
            name = "category",
            field = @AdminField(template = "admin/widget/custom-toone", maxPreloadedOptions = 2)
    ))
    private static final class CustomTemplateModel {
    }

    // Admin-model-level custom fields: a two-arg setter and a reader-only computed property
    static class NoteAdminModel {

        private final Map<TestPost, String> notes = new HashMap<>();

        public String getNote(TestPost post) {
            return notes.getOrDefault(post, "");
        }

        public void setNote(TestPost post, String value) {
            notes.put(post, value);
        }

        public String preview(TestPost post) {
            return "Preview";
        }
    }
}
