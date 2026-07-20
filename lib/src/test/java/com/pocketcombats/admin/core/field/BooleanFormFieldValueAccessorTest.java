package com.pocketcombats.admin.core.field;

import com.pocketcombats.admin.test.TestFields;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BooleanFormFieldValueAccessorTest {

    private static final String INVALID_VALUE_CODE = "spring-jpa-admin.validation.constraints.ValidValue.message";

    @Test
    void missingParameterMeansUnchecked() {
        Article article = new Article();
        BindingResult binding = binding(article);

        accessor().setValue(article, null, binding);

        assertFalse(binding.hasErrors());
        assertEquals(Boolean.FALSE, article.getPublished());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void submittedValueIsApplied(boolean value) {
        Article article = new Article();
        BindingResult binding = binding(article);

        accessor().setValue(article, String.valueOf(value), binding);

        assertFalse(binding.hasErrors());
        assertEquals(value, article.getPublished());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "TRUE", "1", "garbage"})
    void unexpectedValueIsRejectedWithoutModifyingField(String value) {
        Article article = new Article();
        BindingResult binding = binding(article);

        accessor().setValue(article, value, binding);

        assertTrue(binding.hasFieldErrors("published"));
        assertEquals(
                INVALID_VALUE_CODE,
                Objects.requireNonNull(binding.getFieldError("published")).getCode()
        );
        assertEquals(Boolean.TRUE, article.getPublished(), "rejected value must not modify the field");
    }

    private static BooleanFormFieldValueAccessor accessor() {
        return new BooleanFormFieldValueAccessor(
                "published",
                TestFields.reader(Article.class, "published"),
                TestFields.writer(Article.class, "published")
        );
    }

    private static BindingResult binding(Article article) {
        return new BeanPropertyBindingResult(article, "article");
    }

    static class Article {

        private @Nullable Boolean published = Boolean.TRUE;

        public @Nullable Boolean getPublished() {
            return published;
        }
    }
}
