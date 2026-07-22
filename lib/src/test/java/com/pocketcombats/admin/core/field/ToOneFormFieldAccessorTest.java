package com.pocketcombats.admin.core.field;

import com.pocketcombats.admin.core.EntityOptionMapper;
import com.pocketcombats.admin.core.formatter.ToStringValueFormatter;
import com.pocketcombats.admin.core.search.SearchPredicateFactory;
import com.pocketcombats.admin.data.form.AdminSelectOption;
import com.pocketcombats.admin.data.form.AdminSelectOptionsResponse;
import com.pocketcombats.admin.test.JpaTestHarness;
import com.pocketcombats.admin.test.JpaTestUtils;
import com.pocketcombats.admin.test.TestCategory;
import com.pocketcombats.admin.test.TestFields;
import com.pocketcombats.admin.test.TestPost;
import com.pocketcombats.admin.widget.Option;
import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.SingularAttribute;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class ToOneFormFieldAccessorTest {

    private static final int THRESHOLD = 2;
    // Large enough that the truncation tests below never hit it, mirroring the production default.
    private static final int MAX_COUNTED = 1000;

    @RegisterExtension
    static JpaTestHarness jpa = JpaTestHarness.withDefaultEntities();

    private EntityManager em;

    @BeforeEach
    void openEntityManager() {
        em = jpa.em();
    }

    private ToOneFormFieldAccessor accessor(boolean optional) {
        return accessor(optional, true, THRESHOLD);
    }

    private ToOneFormFieldAccessor accessor(boolean optional, boolean autocompleteCapable, int threshold) {
        return accessor(optional, autocompleteCapable, threshold, MAX_COUNTED);
    }

    private ToOneFormFieldAccessor accessor(
            boolean optional,
            boolean autocompleteCapable,
            int threshold,
            int maxCountedOptions
    ) {
        return accessor(optional, autocompleteCapable, threshold, maxCountedOptions, null);
    }

    private ToOneFormFieldAccessor accessor(
            boolean optional,
            boolean autocompleteCapable,
            int threshold,
            int maxCountedOptions,
            @Nullable SearchPredicateFactory search
    ) {
        return new ToOneFormFieldAccessor(
                em,
                new DefaultConversionService(),
                categoryAttribute(),
                optional,
                autocompleteCapable,
                threshold,
                maxCountedOptions,
                TestFields.reader(TestPost.class, "category"),
                TestFields.writer(TestPost.class, "category"),
                optionMapper(),
                search
        );
    }

    private EntityOptionMapper optionMapper() {
        return new EntityOptionMapper(em, new DefaultConversionService(), new ToStringValueFormatter());
    }

    private SingularAttribute<?, ?> categoryAttribute() {
        return em.getMetamodel().entity(TestPost.class).getSingularAttribute("category");
    }

    // Categories get ids 1..count named "Category <id>".
    private static void seedCategories(int count) {
        JpaTestUtils.seedCategories(jpa.emf(), count);
    }

    // Post 1 referencing the given category.
    private static void seedPost(long categoryId) {
        JpaTestUtils.inTransaction(jpa.emf(), tx ->
                tx.persist(new TestPost(1L, tx.getReference(TestCategory.class, categoryId)))
        );
    }

    private static BindingResult binding(TestPost post) {
        return new BeanPropertyBindingResult(post, "post");
    }

    @Test
    void largeTargetTableRendersAutocompleteWithoutPreloadingOptions() {
        seedCategories(THRESHOLD + 1);

        Map<String, ?> attributes = accessor(true).getModelAttributes(new TestPost(1L));

        assertEquals(Boolean.TRUE, attributes.get("_autocomplete"));
        assertEquals("", attributes.get("_currentId"), "a fresh entity means no current selection");
        assertFalse(attributes.containsKey("_options"), "options must not be preloaded for large tables");
    }

    @Test
    void smallTargetTableRendersPreloadedOptionsWithEmptySentinel() {
        seedCategories(THRESHOLD);

        Map<String, ?> attributes = accessor(true).getModelAttributes(new TestPost(1L));

        assertFalse(attributes.containsKey("_autocomplete"));
        List<?> options = (List<?>) attributes.get("_options");
        assertEquals(THRESHOLD + 1, options.size(), "expected empty sentinel plus one option per row");
        assertEquals(Option.EMPTY, options.get(0));
    }

    @Test
    void requiredRelationRendersPreloadedOptionsWithoutEmptySentinel() {
        seedCategories(THRESHOLD);

        Map<String, ?> attributes = accessor(false).getModelAttributes(new TestPost(1L));

        assertEquals(
                Set.of(new Option("id1", "Category 1"), new Option("id2", "Category 2")),
                Set.copyOf((List<?>) attributes.get("_options"))
        );
    }

    @Test
    void negativeThresholdDisablesAutocompleteAndPreloadsAllOptions() {
        int rows = THRESHOLD + 2;
        seedCategories(rows);

        Map<String, ?> attributes = accessor(true, true, -1).getModelAttributes(new TestPost(1L));

        assertFalse(attributes.containsKey("_autocomplete"));
        List<?> options = (List<?>) attributes.get("_options");
        assertEquals(rows + 1, options.size(), "every row preloaded (uncapped), plus the empty sentinel");
    }

    @Test
    void ineligibleFieldOverThresholdCapsPreloadAndFlagsTruncation() {
        // An ineligible field (custom template, composite id, or non-searchable target) can't
        // autocomplete, so a large target is preloaded capped at the threshold and flagged partial.
        seedCategories(5);

        Map<String, ?> attributes = accessor(true, false, THRESHOLD).getModelAttributes(new TestPost(1L));

        assertFalse(attributes.containsKey("_autocomplete"));
        assertEquals(Boolean.TRUE, attributes.get("_truncated"));
        assertEquals(THRESHOLD, attributes.get("_shownCount"), "only the first N rows are preloaded");
        assertEquals("5", attributes.get("_totalApprox"));
        assertEquals(THRESHOLD + 1, ((List<?>) attributes.get("_options")).size(),
                "empty sentinel + the first N options");
    }

    @Test
    void truncatedPreloadReportsAnApproximateTotalOnceTheCountProbeIsCapped() {
        // The row count is only probed up to maxCountedOptions; past it the exact total is unknown,
        // so the note reports "N+" rather than scanning the whole table.
        seedCategories(5);

        Map<String, ?> attributes = accessor(true, false, THRESHOLD, 3).getModelAttributes(new TestPost(1L));

        assertEquals(Boolean.TRUE, attributes.get("_truncated"));
        assertEquals((THRESHOLD + 1) + "+", attributes.get("_totalApprox"),
                "total is capped at maxCountedOptions and marked approximate");
    }

    @Test
    void truncatedPreloadKeepsTheCurrentSelectionSelectableBeyondTheCap() {
        // Editing an entity whose relation is past the first N must not drop it: the current option
        // is unioned into the capped list so it stays selected and round-trips on save.
        seedCategories(5);
        seedPost(5L);
        TestPost managed = em.find(TestPost.class, 1L);

        Map<String, ?> attributes = accessor(true, false, THRESHOLD).getModelAttributes(managed);

        List<?> options = (List<?>) attributes.get("_options");
        assertTrue(
                options.stream().map(o -> ((Option) o).id()).anyMatch("id5"::equals),
                "current selection (past the cap) must remain selectable"
        );
        assertEquals(THRESHOLD + 1, attributes.get("_shownCount"), "first N plus the current selection");
    }

    @Test
    void maxValueThresholdPreloadsAllOptionsWithoutOverflowing() {
        seedCategories(2);

        Map<String, ?> attributes = accessor(true, true, Integer.MAX_VALUE).getModelAttributes(new TestPost(1L));

        assertFalse(attributes.containsKey("_autocomplete"));
        assertEquals(3, ((List<?>) attributes.get("_options")).size());
    }

    @Test
    void zeroThresholdAlwaysAutocompletesWithoutProbingTheTargetTable() {
        // Threshold 0 means "always autocomplete": the outcome is predetermined, so the size probe
        // must be skipped altogether.
        seedCategories(THRESHOLD + 1);
        ToOneFormFieldAccessor accessor = new ToOneFormFieldAccessor(
                em,
                new DefaultConversionService(),
                categoryAttribute(),
                true,
                true,
                0,
                MAX_COUNTED,
                TestFields.reader(TestPost.class, "category"),
                TestFields.writer(TestPost.class, "category"),
                optionMapper(),
                null
        ) {
            @Override
            protected int probeTargetCount(int limit) {
                // Not probing IS the contract here, so the seam throws.
                throw new AssertionError("threshold 0 must not probe the target-table size");
            }
        };

        assertEquals(Boolean.TRUE, accessor.getModelAttributes(new TestPost(1L)).get("_autocomplete"));
    }

    @Test
    void autocompleteAttributesIncludeCurrentSelectionLabel() {
        seedCategories(THRESHOLD + 1);
        seedPost(2L);
        TestPost managed = em.find(TestPost.class, 1L);

        Map<String, ?> attributes = accessor(true).getModelAttributes(managed);

        assertEquals(Boolean.TRUE, attributes.get("_autocomplete"));
        assertEquals("id2", attributes.get("_currentId"));
        assertEquals("Category 2", attributes.get("_currentLabel"));
    }

    @Test
    void danglingReferenceRendersRawIdFallbackLabel() {
        // One extra row so the table stays above the threshold after category 2 is deleted below.
        seedCategories(THRESHOLD + 2);
        seedPost(2L);
        // Simulate a dangling to-one reference, the post still carries category id 2, but the row is gone.
        // The form must render (the admin needs it to repair the entity) and the field must still show
        // that a relation is set, so the raw id stands in for the unformattable label.
        JpaTestUtils.inTransaction(jpa.emf(), tx -> {
            tx.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate();
            tx.createNativeQuery("DELETE FROM TestCategory WHERE id = 2").executeUpdate();
            tx.createNativeQuery("SET REFERENTIAL_INTEGRITY TRUE").executeUpdate();
        });
        TestPost managed = em.find(TestPost.class, 1L);

        Map<String, ?> attributes = accessor(true).getModelAttributes(managed);

        assertEquals(Boolean.TRUE, attributes.get("_autocomplete"));
        assertEquals("id2", attributes.get("_currentId"), "the dangling id must still be submitted back");
        assertEquals("#2", attributes.get("_currentLabel"), "the raw id stands in for the missing row's label");
    }

    @Test
    void detachedUninitializedRelationRendersRawIdFallbackWithoutInitializing() {
        seedCategories(THRESHOLD + 1);
        seedPost(2L);
        TestPost detached;
        try (EntityManager loader = jpa.emf().createEntityManager()) {
            detached = loader.find(TestPost.class, 1L);
        }

        Map<String, ?> attributes = accessor(true).getModelAttributes(detached);

        assertEquals(Boolean.TRUE, attributes.get("_autocomplete"));
        assertEquals("id2", attributes.get("_currentId"), "id must be readable without initializing the proxy");
        assertEquals("#2", attributes.get("_currentLabel"), "label must fall back to the raw id, not initialize the proxy");
    }

    @Test
    void autocompleteLabelKeptForDetachedInitializedRelation() {
        seedCategories(THRESHOLD + 1);
        seedPost(2L);
        TestPost detached;
        try (EntityManager loader = jpa.emf().createEntityManager()) {
            detached = loader.find(TestPost.class, 1L);
            detached.getCategory().getName();
        }

        Map<String, ?> attributes = accessor(true).getModelAttributes(detached);

        assertEquals(Boolean.TRUE, attributes.get("_autocomplete"));
        assertEquals("id2", attributes.get("_currentId"));
        assertEquals("Category 2", attributes.get("_currentLabel"),
                "an initialized relation must keep its label after detaching");
    }

    @Test
    void readValueReturnsPrefixedIdOfCurrentRelation() {
        seedCategories(1);
        seedPost(1L);

        assertEquals("id1", accessor(true).readValue(em.find(TestPost.class, 1L)));
    }

    @Test
    void readValueReturnsEmptySentinelForNullRelation() {
        assertEquals(Option.EMPTY.id(), accessor(true).readValue(new TestPost(1L)));
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("autocompleteSupportCases")
    void autocompleteSupportedRequiresCapabilityAndAnEnabledThreshold(
            boolean autocompleteCapable, int threshold, String description, boolean expected) {
        assertEquals(expected, accessor(true, autocompleteCapable, threshold).autocompleteSupported(), description);
    }

    static Stream<Arguments> autocompleteSupportCases() {
        return Stream.of(
                arguments(true, THRESHOLD, "capable target with an enabled threshold", true),
                arguments(false, THRESHOLD, "structurally incapable target", false),
                arguments(true, -1, "autocomplete disabled by a negative threshold", false),
                arguments(true, Integer.MAX_VALUE, "threshold no table can exceed", false)
        );
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "-1"})
    void absentBlankOrSentinelValueClearsOptionalRelation(String value) {
        TestPost post = new TestPost(1L, new TestCategory(9L, "Current"));
        BindingResult binding = binding(post);

        accessor(true).setValue(post, value, binding);

        assertNull(post.getCategory());
        assertFalse(binding.hasErrors());
    }

    @Test
    void blankValueOnRequiredRelationIsRejected() {
        TestCategory current = new TestCategory(9L, "Current");
        TestPost post = new TestPost(1L, current);
        BindingResult binding = binding(post);

        accessor(false).setValue(post, "", binding);

        assertTrue(binding.hasFieldErrors("category"));
        assertSame(current, post.getCategory(), "rejected value must not modify the relation");
    }

    @ParameterizedTest
    // no prefix / unconvertible / empty id / nonexistent
    @ValueSource(strings = {"5", "idxyz", "id", "id999"})
    void setValueRejectsUnresolvableValue(String value) {
        seedCategories(1);
        TestCategory current = new TestCategory(9L, "Current");
        TestPost post = new TestPost(1L, current);
        BindingResult binding = binding(post);

        accessor(false).setValue(post, value, binding);

        assertTrue(binding.hasFieldErrors("category"));
        assertSame(current, post.getCategory(), "rejected value must not modify the relation");
    }

    @Test
    void setValueResolvesEntityReferenceByStrippedId() {
        seedCategories(3);
        TestPost post = new TestPost(1L);
        BindingResult binding = binding(post);

        accessor(false).setValue(post, "id2", binding);

        assertFalse(binding.hasErrors());
        assertEquals("Category 2", post.getCategory().getName());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "-1", "5", "idABC", "id", "id999"})
    void resolveOptionReturnsNullForUnresolvableValues(String value) {
        seedCategories(1);

        assertNull(accessor(true).resolveOption(value));
    }

    @Test
    void resolveOptionReturnsMatchingOptionForValidPrefixedId() {
        seedCategories(3);

        assertEquals(
                new AdminSelectOption("id2", "Category 2"),
                accessor(true).resolveOption("id2")
        );
    }

    @Test
    void collectOptionsPaginatesInIdOrderAndReportsHasMore() {
        seedCategories(3);

        AdminSelectOptionsResponse firstPage = accessor(true).collectOptions(null, 1, 2);
        AdminSelectOptionsResponse secondPage = accessor(true).collectOptions(null, 2, 2);

        assertEquals(
                List.of(new AdminSelectOption("id1", "Category 1"), new AdminSelectOption("id2", "Category 2")),
                firstPage.results()
        );
        assertTrue(firstPage.hasMore());
        assertEquals(List.of(new AdminSelectOption("id3", "Category 3")), secondPage.results());
        assertFalse(secondPage.hasMore());
    }

    @Test
    void collectOptionsHandlesMaxValuePageSizeWithoutOverflowing() {
        seedCategories(3);

        AdminSelectOptionsResponse response = accessor(true).collectOptions(null, 1, Integer.MAX_VALUE);

        assertEquals(3, response.results().size());
        assertFalse(response.hasMore());
    }

    @Test
    void collectOptionsShortCircuitsForOverflowingPage() {
        seedCategories(1);

        AdminSelectOptionsResponse response = accessor(true).collectOptions(null, Integer.MAX_VALUE, 20);

        assertEquals(List.of(), response.results());
        assertFalse(response.hasMore());
    }

    @Test
    void collectOptionsReturnsNoOptionsForUnmatchableQueryWhenNoSearchConfigured() {
        seedCategories(3);

        AdminSelectOptionsResponse response = accessor(true).collectOptions("not-an-id", 1, 20);

        assertEquals(List.of(), response.results());
        assertFalse(response.hasMore());
    }

    @Test
    void collectOptionsFallsBackToIdSearchWhenNoSearchConfigured() {
        seedCategories(3);

        AdminSelectOptionsResponse response = accessor(true).collectOptions("2", 1, 20);

        assertEquals(List.of(new AdminSelectOption("id2", "Category 2")), response.results());
        assertFalse(response.hasMore());
    }

    @Test
    void collectOptionsUsesConfiguredSearchPredicateFactory() {
        seedCategories(3);
        SearchPredicateFactory search = (cb, criteria, from, query) ->
                Optional.of(cb.like(from.get("name"), "%" + query + "%"));

        AdminSelectOptionsResponse response =
                accessor(true, true, THRESHOLD, MAX_COUNTED, search).collectOptions("Category 1", 1, 20);

        assertEquals(List.of(new AdminSelectOption("id1", "Category 1")), response.results());
    }
}
