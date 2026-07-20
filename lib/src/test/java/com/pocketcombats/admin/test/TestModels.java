package com.pocketcombats.admin.test;

import com.pocketcombats.admin.AdminModel;
import com.pocketcombats.admin.core.AdminModelField;
import com.pocketcombats.admin.core.AdminModelFieldset;
import com.pocketcombats.admin.core.AdminModelListField;
import com.pocketcombats.admin.core.AdminModelRegistryImpl;
import com.pocketcombats.admin.core.AdminRegisteredModel;
import com.pocketcombats.admin.core.RegisteredEntityDetails;
import com.pocketcombats.admin.core.links.AdminModelLink;
import com.pocketcombats.admin.core.property.AdminModelPropertyReader;
import com.pocketcombats.admin.core.search.SearchPredicateFactory;
import com.pocketcombats.admin.core.uniqueness.AdminUniqueConstraint;
import jakarta.persistence.metamodel.IdentifiableType;
import jakarta.persistence.metamodel.SingularAttribute;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds real admin model fixtures with neutral defaults, taming the wide
 * {@link AdminRegisteredModel} record.
 */
public final class TestModels {

    private TestModels() {
    }

    /** The {@link AdminModel @AdminModel} annotation with pure defaults. */
    public static AdminModel adminModelDefaults() {
        return Defaults.class.getAnnotation(AdminModel.class);
    }

    public static Builder model(String name, Class<?> entityClass) {
        return new Builder(name, entityClass);
    }

    /** Registry holding the given models in a single category. */
    public static AdminModelRegistryImpl registry(AdminRegisteredModel... models) {
        Map<String, List<AdminRegisteredModel>> categorized = new LinkedHashMap<>();
        categorized.put("test", List.of(models));
        return new AdminModelRegistryImpl(categorized);
    }

    /** List field exposing {@link TestCategory#getName()}. */
    public static AdminModelListField categoryNameField() {
        return new AdminModelListField("name", "Name", "-", new CategoryNameReader(), null, null);
    }

    public static final class Builder {

        private final String name;
        private final Class<?> entityClass;

        private String label;
        private @Nullable IdentifiableType<?> entityType;
        private int pageSize = 20;
        private List<AdminModelListField> listFields = List.of();
        private @Nullable SearchPredicateFactory searchPredicateFactory;
        private List<AdminModelFieldset> fieldsets = List.of();
        private List<AdminModelLink> links = List.of();
        private List<AdminUniqueConstraint> uniqueConstraints = List.of();

        private Builder(String name, Class<?> entityClass) {
            this.name = name;
            this.entityClass = entityClass;
            this.label = name;
        }

        public Builder label(String label) {
            this.label = label;
            return this;
        }

        /** Completes the metamodel part of the entity details, id attribute included. */
        public Builder entityType(IdentifiableType<?> entityType) {
            this.entityType = entityType;
            return this;
        }

        public Builder pageSize(int pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        public Builder listFields(List<AdminModelListField> listFields) {
            this.listFields = List.copyOf(listFields);
            return this;
        }

        public Builder search(@Nullable SearchPredicateFactory searchPredicateFactory) {
            this.searchPredicateFactory = searchPredicateFactory;
            return this;
        }

        /** Form fields, wrapped into a single unlabeled fieldset. */
        public Builder fields(AdminModelField... fields) {
            this.fieldsets = List.of(new AdminModelFieldset(null, List.of(fields), false));
            return this;
        }

        public Builder links(List<AdminModelLink> links) {
            this.links = List.copyOf(links);
            return this;
        }

        public Builder uniqueConstraints(List<AdminUniqueConstraint> uniqueConstraints) {
            this.uniqueConstraints = List.copyOf(uniqueConstraints);
            return this;
        }

        public AdminRegisteredModel build() {
            return new AdminRegisteredModel(
                    name,
                    0,
                    label,
                    new RegisteredEntityDetails(
                            entityClass,
                            entityType,
                            entityType == null ? null : idAttribute(entityType)
                    ),
                    true,
                    true,
                    pageSize,
                    listFields,
                    null,
                    searchPredicateFactory,
                    List.of(),
                    fieldsets,
                    links,
                    Map.of(),
                    uniqueConstraints,
                    null
            );
        }

        private static <X> SingularAttribute<? super X, ?> idAttribute(IdentifiableType<X> entityType) {
            return entityType.getId(entityType.getIdType().getJavaType());
        }
    }

    @AdminModel
    private static final class Defaults {
    }

    private static final class CategoryNameReader implements AdminModelPropertyReader {

        @Override
        public Class<?> getJavaType() {
            return String.class;
        }

        @Override
        public Object getValue(Object instance) {
            return ((TestCategory) instance).getName();
        }
    }
}
