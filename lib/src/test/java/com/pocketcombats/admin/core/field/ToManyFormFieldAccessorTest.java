package com.pocketcombats.admin.core.field;

import com.pocketcombats.admin.core.EntityOptionMapper;
import com.pocketcombats.admin.core.formatter.ToStringValueFormatter;
import com.pocketcombats.admin.test.JpaTestHarness;
import com.pocketcombats.admin.test.JpaTestUtils;
import com.pocketcombats.admin.test.TestCategory;
import com.pocketcombats.admin.test.TestFields;
import com.pocketcombats.admin.test.TestPost;
import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.PluralAttribute;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.pocketcombats.admin.test.TestMessages.INVALID_ID_CODE;
import static org.junit.jupiter.api.Assertions.*;

class ToManyFormFieldAccessorTest {

    @RegisterExtension
    static JpaTestHarness jpa = JpaTestHarness.withDefaultEntities();

    private EntityManager em;

    @BeforeEach
    void openEntityManager() {
        em = jpa.em();
    }

    @Test
    void setValuesReplacesCollectionContent() {
        seedCategoryWithPost();
        TestCategory category = em.find(TestCategory.class, 1L);
        BindingResult binding = binding(category);

        accessor().setValues(category, List.of("2", "3"), binding);

        assertFalse(binding.hasErrors());
        assertEquals(Set.of(2L, 3L), postIds(category));
    }

    @Test
    void setValuesCreatesCollectionWhenAbsent() {
        JpaTestUtils.inTransaction(jpa.emf(), tx -> tx.persist(new TestPost(1L)));
        TestCategory category = new TestCategory(9L, "Detached");
        BindingResult binding = binding(category);

        accessor().setValues(category, List.of("1"), binding);

        assertFalse(binding.hasErrors());
        assertEquals(Set.of(1L), postIds(category));
    }

    @Test
    void emptySubmissionClearsCollection() {
        seedCategoryWithPost();
        TestCategory category = em.find(TestCategory.class, 1L);
        BindingResult binding = binding(category);

        accessor().setValues(category, null, binding);

        assertFalse(binding.hasErrors());
        assertTrue(category.getPosts().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-a-number", ""})
    void unresolvableIdIsRejectedWithoutTouchingCollection(String value) {
        seedCategoryWithPost();
        TestCategory category = em.find(TestCategory.class, 1L);
        BindingResult binding = binding(category);

        accessor().setValues(category, List.of("2", value), binding);

        assertTrue(binding.hasFieldErrors("posts"));
        assertEquals(
                INVALID_ID_CODE,
                binding.getFieldError("posts").getCode()
        );
        assertEquals(Set.of(1L), postIds(category), "rejected submission must not modify the collection");
    }

    @Test
    void nonexistentIdIsRejectedWithoutTouchingCollection() {
        seedCategoryWithPost();
        TestCategory category = em.find(TestCategory.class, 1L);
        BindingResult binding = binding(category);

        accessor().setValues(category, List.of("2", "999"), binding);

        assertTrue(binding.hasFieldErrors("posts"));
        assertEquals(
                INVALID_ID_CODE,
                binding.getFieldError("posts").getCode()
        );
        assertEquals(Set.of(1L), postIds(category), "rejected submission must not modify the collection");
    }

    // Category 1 holding post 1; posts 2 and 3 exist unassigned.
    private void seedCategoryWithPost() {
        JpaTestUtils.seedCategories(jpa.emf(), 1);
        JpaTestUtils.inTransaction(jpa.emf(), tx -> {
            tx.persist(new TestPost(1L, tx.getReference(TestCategory.class, 1L)));
            tx.persist(new TestPost(2L));
            tx.persist(new TestPost(3L));
        });
    }

    private ToManyFormFieldAccessor accessor() {
        return new ToManyFormFieldAccessor(
                em,
                new DefaultConversionService(),
                (PluralAttribute<?, ?, ?>) em.getMetamodel().entity(TestCategory.class).getAttribute("posts"),
                TestFields.reader(TestCategory.class, "posts"),
                TestFields.writer(TestCategory.class, "posts"),
                new EntityOptionMapper(em, new DefaultConversionService(), new ToStringValueFormatter())
        );
    }

    private static BindingResult binding(TestCategory category) {
        return new BeanPropertyBindingResult(category, "category");
    }

    private static Set<Long> postIds(TestCategory category) {
        return category.getPosts().stream().map(TestPost::getId).collect(Collectors.toSet());
    }
}
