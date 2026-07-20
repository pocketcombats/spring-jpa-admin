package com.pocketcombats.admin.core.field;

import com.pocketcombats.admin.test.TestFields;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

import java.time.Instant;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Malformed form input must surface as a field error on the {@link BindingResult} — an escaping
 * conversion exception would fail the whole request with a server error.
 */
class DelegatingAdminFormFieldValueAccessorImplTest {

    private static final String INVALID_VALUE_CODE = "spring-jpa-admin.validation.constraints.ValidValue.message";

    @Test
    void convertsAndWritesSubmittedValue() {
        Article article = new Article();
        BindingResult binding = binding(article);

        accessor("rating").setValue(article, "42", binding);

        assertFalse(binding.hasErrors());
        assertEquals(42, article.getRating());
    }

    @Test
    void missingValueWritesNull() {
        Article article = new Article();
        BindingResult binding = binding(article);

        accessor("rating").setValue(article, null, binding);

        assertFalse(binding.hasErrors());
        assertNull(article.getRating());
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "12.5"})
    void unconvertibleValueIsRejectedWithoutModifyingField(String value) {
        Article article = new Article();
        BindingResult binding = binding(article);

        accessor("rating").setValue(article, value, binding);

        assertTrue(binding.hasFieldErrors("rating"));
        assertEquals(INVALID_VALUE_CODE, fieldErrorCode(binding, "rating"));
        assertEquals(7, article.getRating(), "rejected value must not modify the field");
    }

    @Test
    void datetimeLocalValueWithoutSecondsIsRejected() {
        // A browser datetime-local input submits "2023-06-18T11:00" — no seconds, no offset —
        // which is not a parseable Instant. Reachable without any form tampering.
        Article article = new Article();
        BindingResult binding = binding(article);

        accessor("postTime").setValue(article, "2023-06-18T11:00", binding);

        assertTrue(binding.hasFieldErrors("postTime"));
        assertEquals(INVALID_VALUE_CODE, fieldErrorCode(binding, "postTime"));
        assertEquals(Article.INITIAL_POST_TIME, article.getPostTime(), "rejected value must not modify the field");
    }

    private static DelegatingAdminFormFieldValueAccessorImpl accessor(String fieldName) {
        return new DelegatingAdminFormFieldValueAccessorImpl(
                fieldName,
                new DefaultFormattingConversionService(),
                TestFields.reader(Article.class, fieldName),
                TestFields.writer(Article.class, fieldName)
        );
    }

    private static BindingResult binding(Article article) {
        return new BeanPropertyBindingResult(article, "article");
    }

    private static @Nullable String fieldErrorCode(BindingResult binding, String field) {
        return Objects.requireNonNull(binding.getFieldError(field)).getCode();
    }

    static class Article {

        static final Instant INITIAL_POST_TIME = Instant.parse("2023-01-01T10:00:00Z");

        private @Nullable Integer rating = 7;
        private Instant postTime = INITIAL_POST_TIME;

        public @Nullable Integer getRating() {
            return rating;
        }

        public Instant getPostTime() {
            return postTime;
        }
    }
}
