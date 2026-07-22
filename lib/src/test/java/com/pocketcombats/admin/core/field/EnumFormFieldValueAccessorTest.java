package com.pocketcombats.admin.core.field;

import com.pocketcombats.admin.core.formatter.ToStringValueFormatter;
import com.pocketcombats.admin.test.TestFields;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

import java.util.Objects;

import static com.pocketcombats.admin.test.TestMessages.INVALID_VALUE_CODE;
import static org.junit.jupiter.api.Assertions.*;

class EnumFormFieldValueAccessorTest {

    @Test
    void submittedOrdinalSelectsEnumConstant() {
        Article article = new Article();
        BindingResult binding = binding(article);

        accessor().setValue(article, "1", binding);

        assertFalse(binding.hasErrors());
        assertEquals(Status.PUBLISHED, article.getStatus());
    }

    @Test
    void emptySentinelClearsOptionalValue() {
        Article article = new Article();
        BindingResult binding = binding(article);

        accessor().setValue(article, "-1", binding);

        assertFalse(binding.hasErrors());
        assertNull(article.getStatus());
    }

    @Test
    void emptySentinelOnRequiredFieldIsRejectedInsteadOfCleared() {
        // A required field's select never offers the empty option, so a submitted sentinel is a
        // stale or hand-crafted form — it must produce a field error, not write null.
        Article article = new Article();
        BindingResult binding = binding(article);

        accessor(false).setValue(article, "-1", binding);

        assertTrue(binding.hasFieldErrors("status"));
        assertEquals(
                "jakarta.validation.constraints.NotNull.message",
                Objects.requireNonNull(binding.getFieldError("status")).getCode()
        );
        assertEquals(Status.DRAFT, article.getStatus(), "rejected value must not modify the field");
    }

    // "3" and "-2" are ordinals no constant has — e.g. a stale form submitted after the enum
    // changed; null is a missing request parameter. None of them may escape as an exception.
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "abc", "3", "-2"})
    void unresolvableValueIsRejectedWithoutModifyingField(@Nullable String value) {
        Article article = new Article();
        BindingResult binding = binding(article);

        accessor().setValue(article, value, binding);

        assertTrue(binding.hasFieldErrors("status"));
        assertEquals(
                INVALID_VALUE_CODE,
                Objects.requireNonNull(binding.getFieldError("status")).getCode()
        );
        assertEquals(Status.DRAFT, article.getStatus(), "rejected value must not modify the field");
    }

    private static EnumFormFieldValueAccessor accessor() {
        return accessor(true);
    }

    private static EnumFormFieldValueAccessor accessor(boolean optional) {
        return new EnumFormFieldValueAccessor(
                "status",
                Status.class,
                optional,
                TestFields.reader(Article.class, "status"),
                TestFields.writer(Article.class, "status"),
                new ToStringValueFormatter()
        );
    }

    private static BindingResult binding(Article article) {
        return new BeanPropertyBindingResult(article, "article");
    }

    enum Status {
        DRAFT, PUBLISHED, ARCHIVED
    }

    static class Article {

        private @Nullable Status status = Status.DRAFT;

        public @Nullable Status getStatus() {
            return status;
        }
    }
}
