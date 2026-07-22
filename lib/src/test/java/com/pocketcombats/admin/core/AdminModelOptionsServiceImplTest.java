package com.pocketcombats.admin.core;

import com.pocketcombats.admin.core.field.AdminFormFieldValueAccessor;
import com.pocketcombats.admin.core.field.AutocompleteOptionsAccessor;
import com.pocketcombats.admin.data.form.AdminSelectOption;
import com.pocketcombats.admin.data.form.AdminSelectOptionsResponse;
import com.pocketcombats.admin.test.StubPermissionService;
import com.pocketcombats.admin.test.TestModels;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

class AdminModelOptionsServiceImplTest {

    private static final int PAGE_SIZE = 20;

    private final AutocompleteFormAccessor accessor = mock();
    private final StubPermissionService permissions = new StubPermissionService();

    private AdminModelOptionsServiceImpl service() {
        AdminModelField toOneField = new AdminModelField(
                "category", "Category", null, "admin/widget/toone", true, true, accessor);
        AdminModelField plainField = new AdminModelField(
                "title", "Title", null, "admin/widget/text", true, true, mock(AdminFormFieldValueAccessor.class));

        AdminRegisteredModel source = TestModels.model("post", Post.class).fields(toOneField, plainField).build();
        return new AdminModelOptionsServiceImpl(TestModels.registry(source), permissions, PAGE_SIZE);
    }

    @Test
    void optionsDeniesAccessWhenSourceModelIsNotViewable() {
        permissions.deny("post");

        assertThrows(AccessDeniedException.class, () -> service().options("post", "category", null, 1));
        verifyNoInteractions(accessor);
    }

    @Test
    void resolveDeniesAccessWhenSourceModelIsNotViewable() {
        permissions.deny("post");

        assertThrows(AccessDeniedException.class, () -> service().resolve("post", "category", "id5"));
        verifyNoInteractions(accessor);
    }

    @Test
    void optionsDelegatesToAccessorWithConfiguredPageSize() throws UnknownModelException {
        // Distinguishable content: equality against this proves the accessor's data actually
        // flowed through the service (an empty response would be record-equal to any other).
        AdminSelectOptionsResponse response = new AdminSelectOptionsResponse(
                List.of(new AdminSelectOption("id1", "One"), new AdminSelectOption("id2", "Two")),
                true
        );
        when(accessor.autocompleteSupported()).thenReturn(true);
        when(accessor.collectOptions("cat", 3, PAGE_SIZE)).thenReturn(response);

        assertEquals(response, service().options("post", "category", "cat", 3));
    }

    @Test
    void resolveWrapsResolvedOptionInSinglePageResponse() throws UnknownModelException {
        AdminSelectOption option = new AdminSelectOption("id5", "Five");
        when(accessor.autocompleteSupported()).thenReturn(true);
        when(accessor.resolveOption("id5")).thenReturn(option);

        AdminSelectOptionsResponse actual = service().resolve("post", "category", "id5");

        assertEquals(List.of(option), actual.results());
        assertFalse(actual.hasMore());
    }

    @Test
    void resolveReturnsEmptyResponseForUnresolvableValue() throws UnknownModelException {
        // resolveOption is deliberately left unstubbed: an unresolvable value resolves to null
        when(accessor.autocompleteSupported()).thenReturn(true);

        AdminSelectOptionsResponse actual = service().resolve("post", "category", "id999");

        assertEquals(List.of(), actual.results());
        assertFalse(actual.hasMore());
    }

    @Test
    void unknownFieldIsRejected() {
        UnknownModelException e = assertThrows(
                UnknownModelException.class, () -> service().options("post", "nope", null, 1));

        assertEquals("model 'post' has no field 'nope'", e.getMessage());
    }

    @Test
    void fieldWithoutAutocompleteCapabilityIsRejected() {
        UnknownModelException e = assertThrows(
                UnknownModelException.class, () -> service().options("post", "title", null, 1));

        assertEquals("field 'title' of model 'post' does not serve options", e.getMessage());
    }

    @Test
    void fieldWithAutocompleteUnsupportedIsRejected() {
        when(accessor.autocompleteSupported()).thenReturn(false);

        assertThrows(UnknownModelException.class, () -> service().options("post", "category", null, 1));
        assertThrows(UnknownModelException.class, () -> service().resolve("post", "category", "id5"));
        verify(accessor, never()).collectOptions(any(), anyInt(), anyInt());
        verify(accessor, never()).resolveOption(any());
    }

    private interface AutocompleteFormAccessor extends AdminFormFieldValueAccessor, AutocompleteOptionsAccessor {
    }

    private static final class Post {
    }
}
