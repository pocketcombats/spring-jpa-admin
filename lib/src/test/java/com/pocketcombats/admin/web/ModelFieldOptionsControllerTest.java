package com.pocketcombats.admin.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pocketcombats.admin.core.AdminModelOptionsService;
import com.pocketcombats.admin.core.UnknownModelException;
import com.pocketcombats.admin.data.form.AdminSelectOption;
import com.pocketcombats.admin.data.form.AdminSelectOptionsResponse;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ModelFieldOptionsControllerTest {

    private final StubOptionsService service = new StubOptionsService();
    private final ModelFieldOptionsController controller = new ModelFieldOptionsController(service);
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void idParameterTakesPrecedenceOverSearchQuery() throws Exception {
        // A present id dispatches to resolution, ignoring q/page
        JsonNode body = readJsonResponse(get("/admin/post/field/category/options")
                .param("q", "cat").param("page", "3").param("id", "id5"));

        assertEquals("resolve", body.at("/results/0/id").asText());
        assertEquals("post/category/id5", body.at("/results/0/text").asText());
    }

    @ParameterizedTest
    @NullAndEmptySource
    void absentOrBlankIdDispatchesToPagedOptions(@Nullable String id) throws Exception {
        MockHttpServletRequestBuilder request = get("/admin/post/field/category/options")
                .param("q", "cat").param("page", "2");
        if (id != null) {
            request = request.param("id", id);
        }

        JsonNode body = readJsonResponse(request);

        assertEquals("options", body.at("/results/0/id").asText(), body::toString);
        assertEquals("post/category/cat/2", body.at("/results/0/text").asText(), body::toString);
    }

    @Test
    void unknownModelRespondsWithNotFound() throws Exception {
        service.failure = new UnknownModelException("model 'nope' has no field 'category'");

        // Both dispatch branches (paged options and id resolution) must surface the unknown model
        // as 404, so exercise each.
        mockMvc.perform(get("/admin/nope/field/category/options"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/admin/nope/field/category/options").param("id", "id5"))
                .andExpect(status().isNotFound());
    }

    @Test
    void jsonFieldNamesMatchWhatTheWidgetScriptReads() throws Exception {
        // toone-autocomplete.js reads data.results[].{id,text} and data.hasMore
        JsonNode body = readJsonResponse(get("/admin/post/field/category/options").param("q", "cat"));

        assertTrue(body.has("hasMore"), body::toString);
        assertTrue(body.get("hasMore").asBoolean());
        assertEquals("options", body.at("/results/0/id").asText(), body::toString);
        assertEquals("post/category/cat/1", body.at("/results/0/text").asText(), body::toString);
    }

    // Performs the request, asserts 200, and parses the response body as JSON
    private JsonNode readJsonResponse(MockHttpServletRequestBuilder request) throws Exception {
        String content = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(content);
    }

    // Echoes each call's arguments into the returned option
    private static final class StubOptionsService implements AdminModelOptionsService {

        @Nullable UnknownModelException failure;

        @Override
        public AdminSelectOptionsResponse options(
                String modelName,
                String fieldName,
                @Nullable String query,
                int page
        ) throws UnknownModelException {
            if (failure != null) {
                throw failure;
            }
            return new AdminSelectOptionsResponse(
                    List.of(new AdminSelectOption("options", modelName + "/" + fieldName + "/" + query + "/" + page)),
                    true
            );
        }

        @Override
        public AdminSelectOptionsResponse resolve(
                String modelName,
                String fieldName,
                String value
        ) throws UnknownModelException {
            if (failure != null) {
                throw failure;
            }
            return new AdminSelectOptionsResponse(
                    List.of(new AdminSelectOption("resolve", modelName + "/" + fieldName + "/" + value)),
                    false
            );
        }
    }
}
