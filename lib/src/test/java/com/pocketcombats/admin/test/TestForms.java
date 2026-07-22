package com.pocketcombats.admin.test;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

public final class TestForms {

    private TestForms() {
    }

    /**
     * Builds submitted form data from alternating name/value pairs.
     */
    public static MultiValueMap<String, String> formData(String... pairs) {
        LinkedMultiValueMap<String, String> data = new LinkedMultiValueMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            data.add(pairs[i], pairs[i + 1]);
        }
        return data;
    }
}
