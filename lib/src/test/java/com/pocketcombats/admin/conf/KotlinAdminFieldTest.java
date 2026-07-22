package com.pocketcombats.admin.conf;

import com.pocketcombats.admin.test.JpaTestHarness;
import com.pocketcombats.admin.test.KotlinFieldAccessPost;
import com.pocketcombats.admin.test.KotlinPropertyAccessPost;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@code @AdminField} discovery for Kotlin-authored entities against the real Hibernate
 * metamodel: whichever member the metamodel reports, the annotation on the paired
 * field/getter must still be honored (see {@code src/test/kotlin}).
 */
class KotlinAdminFieldTest {

    @RegisterExtension
    static JpaTestHarness jpa =
            JpaTestHarness.withEntities(KotlinPropertyAccessPost.class, KotlinFieldAccessPost.class);

    @Test
    void bareAnnotationOnPropertyAccessEntityIsFoundOnBackingField() {
        assertEquals("Field-targeted", label(KotlinPropertyAccessPost.class, "title"));
    }

    @Test
    void getterTargetedAnnotationOnPropertyAccessEntityIsFoundDirectly() {
        assertEquals("Getter-targeted", label(KotlinPropertyAccessPost.class, "subtitle"));
    }

    @Test
    void isPrefixedPropertyFallsBackToPrefixedBackingField() {
        assertEquals("Boolean field-targeted", label(KotlinPropertyAccessPost.class, "active"));
    }

    @Test
    void getterTargetedAnnotationOnFieldAccessEntityIsFoundOnGetter() {
        assertEquals("Getter-targeted", label(KotlinFieldAccessPost.class, "name"));
    }

    @Test
    void bareAnnotationOnFieldAccessEntityIsFoundDirectly() {
        assertEquals("Field-targeted", label(KotlinFieldAccessPost.class, "code"));
    }

    private String label(Class<?> entityClass, String fieldName) {
        return TestFieldFactory.forEntity(jpa.em(), entityClass)
                .build()
                .constructListField(fieldName)
                .label();
    }
}
