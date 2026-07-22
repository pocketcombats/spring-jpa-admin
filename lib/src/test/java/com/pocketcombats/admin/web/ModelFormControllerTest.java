package com.pocketcombats.admin.web;

import com.pocketcombats.admin.conf.JpaAdminProperties;
import com.pocketcombats.admin.core.AdminModelEditingResult;
import com.pocketcombats.admin.core.AdminModelFormService;
import com.pocketcombats.admin.data.form.EntityDetails;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.util.List;

import static com.pocketcombats.admin.test.TestForms.formData;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ModelFormControllerTest {

    private static final JpaAdminProperties PROPERTIES = new JpaAdminProperties(
            /* autoConfigurationOrder */ null,
            /* disableHistory */ false,
            /* historySize */ 10,
            /* maxPreloadedOptions */ 100,
            /* maxCountedOptions */ 1000,
            /* autocompletePageSize */ 20,
            /* methodSecurity */ true,
            /* configureSecurity */ true,
            new JpaAdminProperties.Templates("admin/index", "admin/list", "admin/form", "admin/action")
    );

    private final AdminModelFormService formService = mock(AdminModelFormService.class, invocation -> {
        throw new UnsupportedOperationException("not stubbed: " + invocation.getMethod().getName());
    });
    private final ModelFormController controller = new ModelFormController(PROPERTIES, formService);
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setViewResolvers(new InternalResourceViewResolver())
            .build();

    @Test
    void successfulUpdateWithSaveContinueRedirectsToEditForm() throws Exception {
        doReturn(result("1", false)).when(formService)
                .update("post", "1", formData("model-field-name", "Renamed", "save-continue", ""));

        mockMvc.perform(post("/admin/post/edit/1/")
                        .param("model-field-name", "Renamed")
                        .param("save-continue", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/post/edit/1/"));
    }

    @Test
    void successfulUpdateRedirectsToList() throws Exception {
        doReturn(result("1", false)).when(formService).update("post", "1", formData("save", ""));

        mockMvc.perform(post("/admin/post/edit/1/").param("save", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/post/"));
    }

    @Test
    void updateRedirectPercentEncodesReservedCharactersInThePathVariables() throws Exception {
        doReturn(result("a b&c", false)).when(formService)
                .update("post", "a b&c", formData("save-continue", ""));

        mockMvc.perform(post("/admin/post/edit/{id}/", "a b&c").param("save-continue", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/post/edit/a%20b&c/"));
    }

    @Test
    void updateWithErrorsRerendersForm() throws Exception {
        AdminModelEditingResult result = result("1", true);
        doReturn(result).when(formService).update("post", "1", formData("save", ""));

        // No redirect on validation failure
        ModelAndView mav = mockMvc.perform(post("/admin/post/edit/1/").param("save", ""))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("admin/form"))
                .andReturn().getModelAndView();

        assertNotNull(mav);
        assertSame(result.entityDetails(), mav.getModel().get("entity"));
        assertSame(result.bindingResult(), mav.getModel().get("errors"));
    }

    @Test
    void successfulCreateWithSaveContinueRedirectsToEditFormOfNewEntity() throws Exception {
        doReturn(result("42", false)).when(formService)
                .create("post", formData("model-field-name", "Fresh", "save-continue", ""));

        mockMvc.perform(post("/admin/post/create/")
                        .param("model-field-name", "Fresh")
                        .param("save-continue", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/post/edit/42/"));
    }

    @Test
    void successfulCreateRedirectsToList() throws Exception {
        doReturn(result("42", false)).when(formService).create("post", formData("save", ""));

        mockMvc.perform(post("/admin/post/create/").param("save", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/post/"));
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
}
