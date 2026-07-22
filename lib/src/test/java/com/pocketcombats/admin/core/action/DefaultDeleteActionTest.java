package com.pocketcombats.admin.core.action;

import com.pocketcombats.admin.core.AdminRegisteredModel;
import com.pocketcombats.admin.history.NoOpAdminHistoryWriter;
import com.pocketcombats.admin.test.JpaTestHarness;
import com.pocketcombats.admin.test.TestModels;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.ArrayList;
import java.util.List;

import static com.pocketcombats.admin.test.JpaTestUtils.inTransaction;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Runs the default delete action against a real H2-backed persistence unit. The referencing
 * {@link Pet} rows make the FK constraint part of the setup: should the action ever regress to a
 * bulk delete (which bypasses JPA cascades), removing an owner fails on referential integrity.
 */
class DefaultDeleteActionTest {

    @RegisterExtension
    static JpaTestHarness jpa = JpaTestHarness.withEntities(Owner.class, Pet.class);

    @Test
    void deleteRemovesSelectedEntitiesAndCascadedChildrenOnly() {
        inTransaction(jpa.emf(), em -> {
            Owner first = new Owner(1L);
            Owner second = new Owner(2L);
            em.persist(first);
            em.persist(second);
            em.persist(new Pet(11L, first));
            em.persist(new Pet(12L, first));
            em.persist(new Pet(21L, second));
        });

        DefaultDeleteAction action = new DefaultDeleteAction(new NoOpAdminHistoryWriter());
        inTransaction(jpa.emf(), em ->
                // The action service hands the action already-managed entities
                action.run(em, ownerModel(), List.of(em.find(Owner.class, 1L))));

        inTransaction(jpa.emf(), em -> {
            assertNull(em.find(Owner.class, 1L));
            // The JPA REMOVE cascade must delete owner 1's pets — the whole point of not bulk-deleting.
            assertNull(em.find(Pet.class, 11L));
            assertNull(em.find(Pet.class, 12L));
            // The unselected owner and its pet are untouched.
            assertNotNull(em.find(Owner.class, 2L));
            assertNotNull(em.find(Pet.class, 21L));
        });
    }

    private static AdminRegisteredModel ownerModel() {
        return TestModels.model("owner", Owner.class).label("Owner").build();
    }

    @Entity(name = "DeleteActionOwner")
    static class Owner {

        @Id
        Long id;

        @OneToMany(mappedBy = "owner", cascade = CascadeType.REMOVE)
        List<Pet> pets = new ArrayList<>();

        protected Owner() {
        }

        Owner(Long id) {
            this.id = id;
        }
    }

    @Entity(name = "DeleteActionPet")
    static class Pet {

        @Id
        Long id;

        @ManyToOne(optional = false, fetch = FetchType.LAZY)
        Owner owner;

        protected Pet() {
        }

        Pet(Long id, Owner owner) {
            this.id = id;
            this.owner = owner;
        }
    }
}
