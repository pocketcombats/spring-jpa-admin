package com.pocketcombats.admin.core.links;

import com.pocketcombats.admin.core.AdminModelListEntityMapper;
import com.pocketcombats.admin.core.AdminModelListField;
import com.pocketcombats.admin.core.AdminRegisteredModel;
import com.pocketcombats.admin.core.UnknownModelException;
import com.pocketcombats.admin.data.form.AdminRelationLink;
import com.pocketcombats.admin.data.form.AdminRelationPreview;
import com.pocketcombats.admin.data.list.EntityRelation;
import com.pocketcombats.admin.data.list.Parent;
import com.pocketcombats.admin.test.JpaTestHarness;
import com.pocketcombats.admin.test.JpaTestUtils;
import com.pocketcombats.admin.test.StubPermissionService;
import com.pocketcombats.admin.test.TestCategory;
import com.pocketcombats.admin.test.TestModels;
import com.pocketcombats.admin.test.TestPost;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AdminRelationLinkServiceTest {

    @RegisterExtension
    static JpaTestHarness jpa = JpaTestHarness.withDefaultEntities();

    private final StubPermissionService permissions = new StubPermissionService();
    private final DefaultConversionService conversionService = new DefaultConversionService();
    private EntityManager em;

    @BeforeEach
    void setUp() {
        em = jpa.em();
        JpaTestUtils.seedCategories(jpa.emf(), 1);
    }

    @Test
    void parentPreviewShowsFirstListFieldValue() throws UnknownModelException {
        AdminRelationLinkService service = service(categoryModel(List.of(TestModels.categoryNameField()), List.of()));

        Parent parent = service.getParentInfo(new EntityRelation("category", "1"));

        assertEquals("category label", parent.label());
        assertEquals("category", parent.modelName());
        assertEquals(new AdminRelationPreview("1", "Category 1"), parent.entity());
    }

    @Test
    void parentPreviewFallsBackToEntityIdWhenModelHasNoListFields() throws UnknownModelException {
        AdminRelationLinkService service = service(categoryModel(List.of(), List.of()));

        Parent parent = service.getParentInfo(new EntityRelation("category", "1"));

        assertEquals(new AdminRelationPreview("1", "1"), parent.entity());
    }

    @Test
    void staleParentIdIsReportedAsNotFound() {
        AdminRelationLinkService service = service(categoryModel(List.of(TestModels.categoryNameField()), List.of()));

        assertThrows(
                UnknownModelException.class,
                () -> service.getParentInfo(new EntityRelation("category", "99"))
        );
    }

    @Test
    void parentModelRequiresViewPermission() {
        permissions.deny("category");
        AdminRelationLinkService service = service(categoryModel(List.of(TestModels.categoryNameField()), List.of()));

        assertThrows(
                AccessDeniedException.class,
                () -> service.getParentInfo(new EntityRelation("category", "1"))
        );
    }

    @Test
    void relationLinksResolveTargetModelByEntityClass() {
        // The registered model name deliberately differs from the target class simple name.
        AdminRegisteredModel categoryModel = categoryModel(List.of(TestModels.categoryNameField()), List.of(postsLink(0)));
        AdminRelationLinkService service = service(categoryModel, postsModel());

        List<AdminRelationLink> links = service.collectRelationLinks(categoryModel, category());

        assertEquals(List.of(new AdminRelationLink("posts label", "posts", List.of())), links);
    }

    @Test
    void linkToUnregisteredEntityClassIsSkipped() {
        AdminRegisteredModel categoryModel = categoryModel(List.of(TestModels.categoryNameField()), List.of(postsLink(0)));
        AdminRelationLinkService service = service(categoryModel);

        assertEquals(List.of(), service.collectRelationLinks(categoryModel, category()));
    }

    @Test
    void relationLinksOmitTargetsTheUserCannotView() {
        permissions.deny("posts");
        AdminRegisteredModel categoryModel = categoryModel(List.of(TestModels.categoryNameField()), List.of(postsLink(0)));
        AdminRelationLinkService service = service(categoryModel, postsModel());

        assertEquals(List.of(), service.collectRelationLinks(categoryModel, category()));
    }

    @Test
    void relationPreviewsFallBackToEntityIdWhenTargetHasNoListFields() {
        JpaTestUtils.inTransaction(
                jpa.emf(),
                txEm -> txEm.persist(new TestPost(10L, txEm.getReference(TestCategory.class, 1L)))
        );
        AdminRegisteredModel categoryModel = categoryModel(List.of(TestModels.categoryNameField()), List.of(postsLink(5)));
        AdminRelationLinkService service = service(categoryModel, postsModel());

        List<AdminRelationLink> links = service.collectRelationLinks(categoryModel, category());

        assertEquals(List.of(new AdminRelationPreview("10", "10")), links.get(0).entities());
    }

    private TestCategory category() {
        return em.find(TestCategory.class, 1L);
    }

    private AdminModelLink postsLink(int preview) {
        return new AdminModelLink(null, TestPost.class, new LinkPredicateFactory(em, "category"), preview, null, null, null);
    }

    private AdminRegisteredModel categoryModel(List<AdminModelListField> listFields, List<AdminModelLink> links) {
        return model("category", TestCategory.class, listFields, links);
    }

    private AdminRegisteredModel postsModel() {
        return model("posts", TestPost.class, List.of(), List.of());
    }

    private AdminRelationLinkService service(AdminRegisteredModel... models) {
        return new AdminRelationLinkService(
                TestModels.registry(models),
                new AdminModelListEntityMapper(em, conversionService),
                em,
                permissions,
                conversionService
        );
    }

    private AdminRegisteredModel model(
            String name,
            Class<?> entityClass,
            List<AdminModelListField> listFields,
            List<AdminModelLink> links
    ) {
        return TestModels.model(name, entityClass)
                .label(name + " label")
                .entityType(em.getMetamodel().entity(entityClass))
                .listFields(listFields)
                .links(links)
                .build();
    }
}
