package com.pocketcombats.admin.core;

import com.pocketcombats.admin.test.TestModels;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminModelRegistryImplTest {

    @Test
    void findByEntityClassPrefersTheDefaultNamedRegistration() {
        // "AAACustom" sorts before "SharedEntity", so lexicographic order alone would pick it:
        // the registration named after the entity class must win regardless of name order —
        // and regardless of registration order, so both orders are exercised.
        AdminRegisteredModel custom = TestModels.model("AAACustom", SharedEntity.class).build();
        AdminRegisteredModel defaultNamed = TestModels.model("SharedEntity", SharedEntity.class).build();

        assertSame(defaultNamed,
                TestModels.registry(custom, defaultNamed).findByEntityClass(SharedEntity.class).orElseThrow());
        assertSame(defaultNamed,
                TestModels.registry(defaultNamed, custom).findByEntityClass(SharedEntity.class).orElseThrow());
    }

    @Test
    void findByEntityClassBreaksCustomNameTiesLexicographically() {
        // No registration carries the entity's simple name: the smallest model name is the
        // primary, independent of registration order.
        AdminRegisteredModel beta = TestModels.model("beta", SharedEntity.class).build();
        AdminRegisteredModel alpha = TestModels.model("alpha", SharedEntity.class).build();

        assertSame(alpha, TestModels.registry(beta, alpha).findByEntityClass(SharedEntity.class).orElseThrow());
        assertSame(alpha, TestModels.registry(alpha, beta).findByEntityClass(SharedEntity.class).orElseThrow());
    }

    @Test
    void findAllByEntityClassListsAllRegistrationsPrimaryFirst() {
        AdminRegisteredModel custom = TestModels.model("custom", SharedEntity.class).build();
        AdminRegisteredModel defaultNamed = TestModels.model("SharedEntity", SharedEntity.class).build();

        AdminModelRegistryImpl registry = TestModels.registry(custom, defaultNamed);

        assertEquals(List.of(defaultNamed, custom), registry.findAllByEntityClass(SharedEntity.class));
        assertEquals(List.of(), registry.findAllByEntityClass(OtherEntity.class));
    }

    @Test
    void findByEntityClassIsEmptyForUnregisteredEntity() {
        AdminModelRegistryImpl registry = new AdminModelRegistryImpl(new LinkedHashMap<>());

        assertTrue(registry.findByEntityClass(SharedEntity.class).isEmpty());
    }

    @Test
    void findByEntityClassDoesNotMatchSubtypeRegistrations() {
        // A field declared as a supertype of a registered entity must not borrow the subtype
        // model's configuration: its search predicates may reference subtype-only attributes,
        // and its options would silently diverge from the preloaded select.
        AdminRegisteredModel model = TestModels.model("concrete", ConcreteEntity.class).build();

        assertTrue(TestModels.registry(model).findByEntityClass(BaseEntity.class).isEmpty());
    }

    @Test
    void duplicateModelNameIsRejected() {
        AdminRegisteredModel first = TestModels.model("dup", SharedEntity.class).build();
        AdminRegisteredModel second = TestModels.model("dup", OtherEntity.class).build();

        IllegalStateException e = assertThrows(
                IllegalStateException.class,
                () -> TestModels.registry(first, second)
        );
        assertTrue(e.getMessage().contains("dup"));
    }

    private static final class SharedEntity {
    }

    private static class BaseEntity {
    }

    private static final class ConcreteEntity extends BaseEntity {
    }

    private static final class OtherEntity {
    }
}
