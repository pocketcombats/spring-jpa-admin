package com.pocketcombats.admin.conf;

import com.pocketcombats.admin.core.formatter.SpelExpressionContextFactory;
import com.pocketcombats.admin.test.JpaTestUtils;
import com.pocketcombats.admin.test.KotlinFieldAccessPost;
import com.pocketcombats.admin.test.KotlinPropertyAccessPost;
import com.pocketcombats.admin.test.TestModels;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.support.DefaultConversionService;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@code @AdminField} discovery for Kotlin-authored entities against the real Hibernate
 * metamodel: whichever member the metamodel reports, the annotation on the paired
 * field/getter must still be honored (see {@code src/test/kotlin}).
 */
class KotlinAdminFieldTest {

    private static EntityManagerFactory emf;

    private EntityManager em;

    @BeforeAll
    static void createEntityManagerFactory() {
        emf = JpaTestUtils.createEntityManagerFactory(KotlinPropertyAccessPost.class, KotlinFieldAccessPost.class);
    }

    @AfterAll
    static void closeEntityManagerFactory() {
        emf.close();
    }

    @BeforeEach
    void openEntityManager() {
        em = emf.createEntityManager();
    }

    @AfterEach
    void closeEntityManager() {
        em.close();
    }

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
        FieldFactory factory = new FieldFactory(
                em,
                new DefaultConversionService(),
                new SpelExpressionContextFactory(),
                entityClass.getSimpleName(), TestModels.adminModelDefaults(), entityClass, em.getMetamodel().entity(entityClass), null
        );
        return factory.constructListField(fieldName).label();
    }
}
