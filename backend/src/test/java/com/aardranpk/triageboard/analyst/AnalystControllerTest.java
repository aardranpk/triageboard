package com.aardranpk.triageboard.analyst;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.aardranpk.triageboard.common.DuplicateResourceException;

@WebMvcTest(AnalystController.class)
class AnalystControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnalystService analystService;

    @Test
    void createRejectsInvalidEmail() throws Exception {
        mockMvc.perform(post("/api/analysts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Alex Chen", "email": "not-an-email"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists());

        verifyNoInteractions(analystService);
    }

    @Test
    void createReturns409ForDuplicateEmail() throws Exception {
        when(analystService.create(any(CreateAnalystRequest.class)))
                .thenThrow(new DuplicateResourceException(
                        "Analyst with email alex.chen@example.com already exists"));

        mockMvc.perform(post("/api/analysts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Alex Chen", "email": "alex.chen@example.com"}
                                """))
                .andExpect(status().isConflict());
    }
}