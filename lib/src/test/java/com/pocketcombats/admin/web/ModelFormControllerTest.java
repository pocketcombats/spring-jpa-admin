package com.pocketcombats.admin.web;

import com.pocketcombats.admin.conf.JpaAdminProperties;
import com.pocketcombats.admin.core.AdminModelEditingResult;
import com.pocketcombats.admin.core.AdminModelFormService;
import com.pocketcombats.admin.data.form.EntityDetails;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ModelFormControllerTest {

    private static final JpaAdminProperties PROPERTIES = new JpaAdminProperties(
            null, false, 10, true, true,
            new JpaAdminProperties.Templates("admin/index", "admin/list", "admin/form", "admin/action")
    );

    private final StubFormService formService = new StubFormService();
    private final ModelFormController controller = new ModelFormController(PROPERTIES, formService);

    @Test
    void successfulUpdateWithSaveContinueRedirectsToEditForm() throws Exception {
        formService.updateResult = result("1", false);

        ModelAndView mav = controller.update("post", "1", formData("save-continue"));

        // Redirect (POST-redirect-GET) so a browser refresh doesn't re-submit the form.
        // A URI template, not a concatenated URL: RedirectView re-expands {model}/{id} from the
        // request's path variables and percent-encodes them, which keeps string ids with
        // reserved characters intact.
        assertEquals("redirect:/admin/{model}/edit/{id}/", mav.getViewName());
    }

    @Test
    void successfulUpdateRedirectsToList() throws Exception {
        formService.updateResult = result("1", false);

        ModelAndView mav = controller.update("post", "1", formData("save"));

        assertEquals("redirect:/admin/{model}/", mav.getViewName());
    }

    @Test
    void updateWithErrorsRerendersForm() throws Exception {
        formService.updateResult = result("1", true);

        ModelAndView mav = controller.update("post", "1", formData("save"));

        assertEquals("admin/form", mav.getViewName());
        assertSame(formService.updateResult.entityDetails(), mav.getModel().get("entity"));
        assertSame(formService.updateResult.bindingResult(), mav.getModel().get("errors"));
    }

    @Test
    void successfulCreateWithSaveContinueRedirectsToEditFormOfNewEntity() throws Exception {
        formService.createResult = result("42", false);

        ModelAndView mav = controller.create("post", formData("save-continue"));

        assertEquals("redirect:/admin/{model}/edit/{id}/", mav.getViewName());
        // The new entity's id is not a request path variable; it must be supplied for expansion
        assertEquals("42", mav.getModel().get("id"));
    }

    @Test
    void successfulCreateRedirectsToList() throws Exception {
        formService.createResult = result("42", false);

        ModelAndView mav = controller.create("post", formData("save"));

        assertEquals("redirect:/admin/{model}/", mav.getViewName());
    }

    private static AdminModelEditingResult result(String id, boolean hasErrors) {
        BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "post");
        if (hasErrors) {
            bindingResult.reject("test.error");
        }
        return new AdminModelEditingResult(
                new EntityDetails("post", id, "Post", List.of(), List.of(), true),
                bindingResult
        );
    }

    private static MultiValueMap<String, String> formData(String submitButton) {
        LinkedMultiValueMap<String, String> data = new LinkedMultiValueMap<>();
        data.add(submitButton, "");
        return data;
    }

    // Returns canned results; everything the controller must not call throws.
    private static final class StubFormService implements AdminModelFormService {

        @Nullable AdminModelEditingResult updateResult;
        @Nullable AdminModelEditingResult createResult;

        @Override
        public EntityDetails details(String modelName, String id) {
            throw new UnsupportedOperationException("not stubbed");
        }

        @Override
        public AdminModelEditingResult update(String modelName, String id, MultiValueMap<String, String> data) {
            return Objects.requireNonNull(updateResult);
        }

        @Override
        public BindingResult updateField(String modelName, String stringId, String fieldName, @Nullable String value) {
            throw new UnsupportedOperationException("not stubbed");
        }

        @Override
        public EntityDetails create(String modelName) {
            throw new UnsupportedOperationException("not stubbed");
        }

        @Override
        public AdminModelEditingResult create(String modelName, MultiValueMap<String, String> data) {
            return Objects.requireNonNull(createResult);
        }
    }
}
