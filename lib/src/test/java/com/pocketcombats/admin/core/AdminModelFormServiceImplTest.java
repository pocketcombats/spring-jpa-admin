package com.pocketcombats.admin.core;

import com.pocketcombats.admin.core.field.AdminFormFieldPluralValueAccessor;
import com.pocketcombats.admin.core.field.AdminFormFieldSingularValueAccessor;
import com.pocketcombats.admin.core.field.AdminFormFieldValueAccessor;
import com.pocketcombats.admin.core.field.DelegatingAdminFormFieldValueAccessorImpl;
import com.pocketcombats.admin.core.field.ToManyFormFieldAccessor;
import com.pocketcombats.admin.core.formatter.ToStringValueFormatter;
import com.pocketcombats.admin.core.links.AdminRelationLinkService;
import com.pocketcombats.admin.core.permission.AdminPermissionService;
import com.pocketcombats.admin.core.uniqueness.AdminUniqueConstraint;
import com.pocketcombats.admin.data.form.AdminFormField;
import com.pocketcombats.admin.data.form.EntityDetails;
import com.pocketcombats.admin.history.AdminHistoryWriter;
import com.pocketcombats.admin.history.NoOpAdminHistoryWriter;
import com.pocketcombats.admin.test.JpaTestUtils;
import com.pocketcombats.admin.test.StubPermissionService;
import com.pocketcombats.admin.test.TestCategory;
import com.pocketcombats.admin.test.TestFields;
import com.pocketcombats.admin.test.TestModels;
import com.pocketcombats.admin.test.TestPost;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.ObjectError;
import org.springframework.validation.SmartValidator;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercises the form service against a real H2-backed {@link EntityManager}: form values are bound
 * through hand-rolled reflective accessors and outcomes are asserted as entity/database state.
 */
class AdminModelFormServiceImplTest {

    private static EntityManagerFactory emf;

    private EntityManager em;

    @BeforeAll
    static void createFactory() {
        emf = JpaTestUtils.createEntityManagerFactory();
    }

    @AfterAll
    static void closeFactory() {
        emf.close();
    }

    @BeforeEach
    void setUp() {
        JpaTestUtils.wipeData(emf);
        JpaTestUtils.seedCategories(emf, 2);
        em = emf.createEntityManager();
    }

    @AfterEach
    void tearDown() {
        em.close();
    }

    @Test
    void updateWithValidationErrorKeepsEditIdentity() throws Exception {
        TestableFormService service = service(categoryModel(List.of()));

        AdminModelEditingResult result = inServiceTransaction(service, () ->
                service.update("category", "1", formData("model-field-name", ReflectiveFieldAccessor.REJECTED_VALUE)));

        assertTrue(result.bindingResult().hasErrors());
        // A null id would make the re-rendered form post to the create endpoint,
        // inserting a duplicate row on retry
        assertEquals("1", result.entityDetails().id());
        assertTrue(service.rollbackRequested);
    }

    @Test
    void updateAppliesChangesAndKeepsEditIdentity() throws Exception {
        TestableFormService service = service(categoryModel(List.of()));

        AdminModelEditingResult result = inServiceTransaction(service, () ->
                service.update("category", "1", formData("model-field-name", "Renamed")));

        assertFalse(result.bindingResult().hasErrors());
        assertEquals("1", result.entityDetails().id());
        assertFalse(service.rollbackRequested);
        assertEquals("Renamed", categoryName(1L));
    }

    @Test
    void createWithValidationErrorKeepsCreateIdentity() throws Exception {
        TestableFormService service = service(categoryModel(List.of()));

        AdminModelEditingResult result = inServiceTransaction(service, () ->
                service.create("category", formData(
                        "model-field-id", "7",
                        "model-field-name", ReflectiveFieldAccessor.REJECTED_VALUE
                )));

        assertTrue(result.bindingResult().hasErrors());
        assertNull(result.entityDetails().id());
        assertTrue(service.rollbackRequested);
    }

    @Test
    void createdEntityExposesItsNewId() throws Exception {
        TestableFormService service = service(categoryModel(List.of()));

        AdminModelEditingResult result = inServiceTransaction(service, () ->
                service.create("category", formData(
                        "model-field-id", "7",
                        "model-field-name", "Fresh"
                )));

        assertFalse(result.bindingResult().hasErrors());
        assertEquals("7", result.entityDetails().id());
        assertEquals("Fresh", categoryName(7L));
    }

    @ParameterizedTest
    @ValueSource(strings = {"999", "not-a-number"})
    void staleOrMalformedIdIsReportedAsNotFound(String stringId) {
        TestableFormService service = service(categoryModel(List.of()));

        UnknownModelException e = assertThrows(
                UnknownModelException.class,
                () -> service.details("category", stringId)
        );
        assertTrue(Objects.requireNonNull(e.getMessage()).contains(stringId));
    }

    @Test
    void detailsResolvesExistingEntity() throws Exception {
        TestableFormService service = service(categoryModel(List.of()));

        EntityDetails details = service.details("category", "2");

        assertEquals("2", details.id());
    }

    @Test
    void errorReRenderReadsFieldsFromTheStillManagedEntity() throws Exception {
        JpaTestUtils.inTransaction(emf, seeder ->
                seeder.persist(new TestPost(1L, seeder.find(TestCategory.class, 1L))));
        ManagedStateRecordingAccessor readOnlyAccessor = new ManagedStateRecordingAccessor("category");
        TestableFormService service = service(postModel(
                new AdminModelField("category", "Category", null, "admin/widget/text", true, false, readOnlyAccessor),
                new AdminModelField("editable", "Editable", null, "admin/widget/text", true, true, new AlwaysRejectingAccessor("editable"))
        ));

        AdminModelEditingResult result = inServiceTransaction(service, () ->
                service.update("post", "1", formData("model-field-editable", "anything")));

        assertTrue(result.bindingResult().hasErrors());
        assertEquals("1", result.entityDetails().id());
        assertEquals(Boolean.TRUE, readOnlyAccessor.managedOnFirstRead);
    }

    @Test
    void updateWithValidationErrorRendersLazyReadOnlyToManyField() throws Exception {
        JpaTestUtils.inTransaction(emf, seeder ->
                seeder.persist(new TestPost(1L, seeder.find(TestCategory.class, 1L))));
        TestableFormService service = service(postsModel(new ToManyFormFieldAccessor(
                em,
                DefaultConversionService.getSharedInstance(),
                em.getMetamodel().entity(TestCategory.class).getAttribute("posts"),
                TestFields.reader(TestCategory.class, "posts"),
                TestFields.writer(TestCategory.class, "posts"),
                new ToStringValueFormatter()
        )));

        AdminModelEditingResult result = inServiceTransaction(service, () ->
                service.update("category", "1", formData("model-field-name", ReflectiveFieldAccessor.REJECTED_VALUE)));

        assertTrue(result.bindingResult().hasErrors());
        assertTrue(service.rollbackRequested);
        AdminFormField postsField = result.entityDetails().fieldGroups().get(0).fields().stream()
                .filter(field -> field.name().equals("posts"))
                .findFirst()
                .orElseThrow();
        assertEquals(List.of("1"), postsField.value(), "the lazy collection must render its actual content");
    }

    @Test
    void readOnlyToManyFieldIsReadOnlyOnceForTheFinalRender() throws Exception {
        JpaTestUtils.inTransaction(emf, seeder ->
                seeder.persist(new TestPost(1L, seeder.find(TestCategory.class, 1L))));
        PluralReadCountingAccessor postsAccessor = new PluralReadCountingAccessor("posts");
        TestableFormService service = service(postsModel(postsAccessor));

        AdminModelEditingResult result = inServiceTransaction(service, () ->
                service.update("category", "1", formData("model-field-name", "Renamed")));

        assertFalse(result.bindingResult().hasErrors());
        assertEquals(1, postsAccessor.readCount);
    }

    @Test
    void partiallyBoundStateIsRolledBackRatherThanCommitted() throws Exception {
        TestableFormService service = service(TestModels.model("category", TestCategory.class)
                .label("Category")
                .entityType(emf.getMetamodel().entity(TestCategory.class))
                .fields(
                        new AdminModelField("name", "Name", null, "admin/widget/text", true, true,
                                new ReflectiveFieldAccessor(TestCategory.class, "name", value -> value)),
                        new AdminModelField("blocked", "Blocked", null, "admin/widget/text", true, true,
                                new AlwaysRejectingAccessor("blocked"))
                )
                .build());

        AdminModelEditingResult result = inServiceTransaction(service, () ->
                service.update("category", "1", formData(
                        "model-field-name", "Renamed",
                        "model-field-blocked", "anything"
                )));

        assertTrue(result.bindingResult().hasErrors());
        AdminFormField nameField = result.entityDetails().fieldGroups().get(0).fields().stream()
                .filter(field -> field.name().equals("name"))
                .findFirst()
                .orElseThrow();
        assertEquals("Renamed", nameField.value(), "the earlier field is applied before the later one is rejected");
        assertTrue(service.rollbackRequested);
    }

    @Test
    void updateWithUnconvertibleValueYieldsFieldError() throws Exception {
        Instant originalPostTime = Instant.parse("2023-01-01T10:00:00Z");
        seedPostWithPostTime(originalPostTime);
        TestableFormService service = service(postModel(postTimeField()));

        AdminModelEditingResult result = inServiceTransaction(service, () ->
                service.update("post", "1", formData("model-field-postTime", "2023-06-18T11:00")));

        assertTrue(result.bindingResult().hasFieldErrors("postTime"));
        assertEquals(
                "spring-jpa-admin.validation.constraints.ValidValue.message",
                Objects.requireNonNull(result.bindingResult().getFieldError("postTime")).getCode()
        );
        assertEquals("1", result.entityDetails().id());
        assertTrue(service.rollbackRequested);
    }

    @Test
    void updateAppliesConvertedTemporalValue() throws Exception {
        seedPostWithPostTime(Instant.parse("2023-01-01T10:00:00Z"));
        TestableFormService service = service(postModel(postTimeField()));

        AdminModelEditingResult result = inServiceTransaction(service, () ->
                service.update("post", "1", formData("model-field-postTime", "2023-06-18T11:00:00Z")));

        assertFalse(result.bindingResult().hasErrors());
        assertFalse(service.rollbackRequested);
        assertEquals(Instant.parse("2023-06-18T11:00:00Z"), postTime(1L));
    }

    @Test
    void unknownFieldNameIsReportedAsNotFound() {
        TestableFormService service = service(categoryModel(List.of()));

        UnknownModelException e = assertThrows(
                UnknownModelException.class,
                () -> service.updateField("category", "1", "nope", "x")
        );
        assertTrue(Objects.requireNonNull(e.getMessage()).contains("nope"));
    }

    @Test
    void singleFieldUpdateEnforcesUniqueConstraints() throws Exception {
        TestableFormService service = service(categoryModel(List.of(new NameUniqueConstraint("Name"))));

        BindingResult result = inServiceTransaction(service, () ->
                service.updateField("category", "1", "name", "Category 2"));

        assertTrue(result.hasErrors());
        assertEquals(
                "spring-jpa-admin.validation.uniqueness-violation.fields.message",
                result.getGlobalErrors().get(0).getCode()
        );
        assertTrue(service.rollbackRequested);
    }

    @Test
    void singleFieldUpdatePersistsAcceptedValue() throws Exception {
        TestableFormService service = service(categoryModel(List.of(new NameUniqueConstraint("Name"))));

        BindingResult result = inServiceTransaction(service, () ->
                service.updateField("category", "1", "name", "Renamed"));

        assertFalse(result.hasErrors());
        assertEquals("Renamed", categoryName(1L));
    }

    @Test
    void updateKeepingUniqueValueIsNotAConflict() throws Exception {
        TestableFormService service = service(categoryModel(List.of(new NameUniqueConstraint("Name"))));

        AdminModelEditingResult result = inServiceTransaction(service, () ->
                service.update("category", "1", formData("model-field-name", "Category 1")));

        assertFalse(result.bindingResult().hasErrors());
        assertEquals("Category 1", categoryName(1L));
    }

    @Test
    void uniquenessViolationMessageArgsArePlainText() throws Exception {
        TestableFormService service = service(categoryModel(List.of(new NameUniqueConstraint("Category <name>"))));

        AdminModelEditingResult result = inServiceTransaction(service, () ->
                service.update("category", "1", formData("model-field-name", "Category 2")));

        assertTrue(result.bindingResult().hasErrors());
        ObjectError error = result.bindingResult().getGlobalErrors().get(0);
        assertEquals("spring-jpa-admin.validation.uniqueness-violation.fields.message", error.getCode());
        // The message templates are plain text ({1} conflicting id, {2} field labels):
        // args must not be HTML-escaped or markup-wrapped
        assertArrayEquals(new Object[]{"category", "2", "Category <name>"}, error.getArguments());
    }

    @Test
    void uniquenessViolationErrorCarriesConflictCoordinatesForTheViewLink() throws Exception {
        // The message itself is plain text, so navigation to the conflicting entity is carried as
        // structured data on the error and rendered as a real link by the form template.
        TestableFormService service = service(categoryModel(List.of(new NameUniqueConstraint("Name"))));

        AdminModelEditingResult result = inServiceTransaction(service, () ->
                service.update("category", "1", formData("model-field-name", "Category 2")));

        ObjectError error = result.bindingResult().getGlobalErrors().get(0);
        UniquenessViolationError conflict = assertInstanceOf(UniquenessViolationError.class, error);
        assertEquals("category", conflict.getConflictingModel());
        assertEquals("2", conflict.getConflictingEntityId());
    }

    private TestableFormService service(AdminRegisteredModel model) {
        AdminModelRegistryImpl registry = TestModels.registry(model);
        ConversionService conversionService = DefaultConversionService.getSharedInstance();
        AdminPermissionService permissions = new StubPermissionService();
        return new TestableFormService(
                registry,
                new NoOpAdminHistoryWriter(),
                new AdminRelationLinkService(
                        registry,
                        new AdminModelListEntityMapper(em, conversionService),
                        em,
                        permissions,
                        conversionService
                ),
                em,
                new NoOpValidator(),
                permissions,
                conversionService,
                new StaticMessageSource()
        );
    }

    private AdminRegisteredModel categoryModel(List<AdminUniqueConstraint> uniqueConstraints) {
        return TestModels.model("category", TestCategory.class)
                .label("Category")
                .entityType(emf.getMetamodel().entity(TestCategory.class))
                .fields(
                        new AdminModelField("id", "Id", null, "admin/widget/text", true, false,
                                new ReflectiveFieldAccessor(TestCategory.class, "id", Long::valueOf)),
                        new AdminModelField("name", "Name", null, "admin/widget/text", true, true,
                                new ReflectiveFieldAccessor(TestCategory.class, "name", value -> value))
                )
                .uniqueConstraints(uniqueConstraints)
                .build();
    }

    private AdminRegisteredModel postModel(AdminModelField... fields) {
        return TestModels.model("post", TestPost.class)
                .label("Post")
                .entityType(emf.getMetamodel().entity(TestPost.class))
                .fields(fields)
                .build();
    }

    private AdminRegisteredModel postsModel(AdminFormFieldPluralValueAccessor postsAccessor) {
        return TestModels.model("category", TestCategory.class)
                .label("Category")
                .entityType(emf.getMetamodel().entity(TestCategory.class))
                .fields(
                        new AdminModelField("name", "Name", null, "admin/widget/text", true, true,
                                new ReflectiveFieldAccessor(TestCategory.class, "name", value -> value)),
                        new AdminModelField("posts", "Posts", null, "admin/widget/multiselect", true, false,
                                postsAccessor)
                )
                .build();
    }

    /**
     * Runs a service call in its own transaction, completing it the way Spring would:
     * rollback when the service requested it, commit otherwise.
     */
    private <T> T inServiceTransaction(TestableFormService service, Callable<T> call) throws Exception {
        em.getTransaction().begin();
        try {
            T result = call.call();
            if (service.rollbackRequested) {
                em.getTransaction().rollback();
            } else {
                em.getTransaction().commit();
            }
            return result;
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
        }
    }

    private @Nullable String categoryName(long id) {
        try (EntityManager reader = emf.createEntityManager()) {
            TestCategory category = reader.find(TestCategory.class, id);
            return category == null ? null : category.getName();
        }
    }

    private void seedPostWithPostTime(Instant postTime) {
        JpaTestUtils.inTransaction(emf, seeder -> {
            TestPost post = new TestPost(1L, seeder.find(TestCategory.class, 1L));
            post.setPostTime(postTime);
            seeder.persist(post);
        });
    }

    private @Nullable Instant postTime(long id) {
        try (EntityManager reader = emf.createEntityManager()) {
            TestPost post = reader.find(TestPost.class, id);
            return post == null ? null : post.getPostTime();
        }
    }

    // The real accessor + a real conversion service with java.time support: the exact production
    // wiring behind text/datetime widgets.
    private AdminModelField postTimeField() {
        return new AdminModelField("postTime", "Post time", null, "admin/widget/text", true, true,
                new DelegatingAdminFormFieldValueAccessorImpl(
                        "postTime",
                        new DefaultFormattingConversionService(),
                        TestFields.reader(TestPost.class, "postTime"),
                        TestFields.writer(TestPost.class, "postTime")
                ));
    }

    private static MultiValueMap<String, String> formData(String... pairs) {
        LinkedMultiValueMap<String, String> data = new LinkedMultiValueMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            data.add(pairs[i], pairs[i + 1]);
        }
        return data;
    }

    // Captures the rollback decision instead of requiring Spring transaction infrastructure.
    private static final class TestableFormService extends AdminModelFormServiceImpl {

        boolean rollbackRequested;

        TestableFormService(
                AdminModelRegistry modelRegistry,
                AdminHistoryWriter historyWriter,
                AdminRelationLinkService relationLinkService,
                EntityManager em,
                SmartValidator validator,
                AdminPermissionService permissionService,
                ConversionService conversionService,
                StaticMessageSource messageSource
        ) {
            super(modelRegistry, historyWriter, relationLinkService, em, validator,
                    permissionService, conversionService, messageSource);
        }

        @Override
        protected void setRollbackOnly() {
            rollbackRequested = true;
        }
    }

    // Real field access on real entities; rejects the marker value to drive the error path.
    private static class ReflectiveFieldAccessor implements AdminFormFieldSingularValueAccessor {

        static final String REJECTED_VALUE = "!reject";

        private final String name;
        private final Field field;
        private final Function<String, Object> parser;

        ReflectiveFieldAccessor(Class<?> owner, String name, Function<String, Object> parser) {
            this.name = name;
            this.parser = parser;
            this.field = TestFields.field(owner, name);
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getDefaultTemplate() {
            return "admin/widget/text";
        }

        @Override
        public Class<?> getReaderJavaType() {
            return String.class;
        }

        @Override
        public Class<?> getWriterJavaType() {
            return String.class;
        }

        @Override
        public boolean isWritable() {
            return true;
        }

        @Override
        public @Nullable Object readValue(Object instance) {
            try {
                return field.get(instance);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public Map<String, Object> getModelAttributes(Object instance) {
            return Map.of();
        }

        @Override
        public void setValue(Object instance, @Nullable String value, BindingResult bindingResult) {
            if (REJECTED_VALUE.equals(value)) {
                bindingResult.reject("test.rejected");
                return;
            }
            try {
                field.set(instance, value == null ? null : parser.apply(value));
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    // Records whether its first read happened while the entity was still managed.
    private final class ManagedStateRecordingAccessor implements AdminFormFieldValueAccessor {

        private final String name;

        @Nullable Boolean managedOnFirstRead;

        ManagedStateRecordingAccessor(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getDefaultTemplate() {
            return "admin/widget/text";
        }

        @Override
        public Class<?> getReaderJavaType() {
            return String.class;
        }

        @Override
        public Class<?> getWriterJavaType() {
            return Void.TYPE;
        }

        @Override
        public boolean isWritable() {
            return false;
        }

        @Override
        public @Nullable Object readValue(Object instance) {
            if (managedOnFirstRead == null) {
                managedOnFirstRead = em.contains(instance);
            }
            return "read";
        }

        @Override
        public Map<String, Object> getModelAttributes(Object instance) {
            return Map.of();
        }
    }

    private static final class PluralReadCountingAccessor implements AdminFormFieldPluralValueAccessor {

        private final String name;

        int readCount;

        PluralReadCountingAccessor(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getDefaultTemplate() {
            return "admin/widget/multiselect";
        }

        @Override
        public Class<?> getReaderJavaType() {
            return List.class;
        }

        @Override
        public Class<?> getWriterJavaType() {
            return Void.TYPE;
        }

        @Override
        public boolean isWritable() {
            return false;
        }

        @Override
        public @Nullable Object readValue(Object instance) {
            readCount++;
            return List.of();
        }

        @Override
        public Map<String, Object> getModelAttributes(Object instance) {
            return Map.of();
        }

        @Override
        public void setValues(Object instance, @Nullable List<String> values, BindingResult bindingResult) {
            throw new UnsupportedOperationException("read-only field must not be bound");
        }
    }

    private static final class AlwaysRejectingAccessor implements AdminFormFieldSingularValueAccessor {

        private final String name;

        AlwaysRejectingAccessor(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getDefaultTemplate() {
            return "admin/widget/text";
        }

        @Override
        public Class<?> getReaderJavaType() {
            return String.class;
        }

        @Override
        public Class<?> getWriterJavaType() {
            return String.class;
        }

        @Override
        public boolean isWritable() {
            return true;
        }

        @Override
        public @Nullable Object readValue(Object instance) {
            return null;
        }

        @Override
        public Map<String, Object> getModelAttributes(Object instance) {
            return Map.of();
        }

        @Override
        public void setValue(Object instance, @Nullable String value, BindingResult bindingResult) {
            bindingResult.reject("test.rejected");
        }
    }

    private static final class NameUniqueConstraint implements AdminUniqueConstraint {

        private final String label;

        NameUniqueConstraint(String label) {
            this.label = label;
        }

        @Override
        public Predicate createPredicate(CriteriaBuilder cb, Root<?> root, Object entity) {
            return cb.equal(root.get("name"), ((TestCategory) entity).getName());
        }

        @Override
        public boolean matches(Object entityA, Object entityB) {
            return Objects.equals(((TestCategory) entityA).getName(), ((TestCategory) entityB).getName());
        }

        @Override
        public List<String> getFieldLabels() {
            return List.of(label);
        }
    }

    private static final class NoOpValidator implements SmartValidator {

        @Override
        public boolean supports(Class<?> clazz) {
            return true;
        }

        @Override
        public void validate(Object target, Errors errors) {
        }

        @Override
        public void validate(Object target, Errors errors, Object... validationHints) {
        }
    }
}
