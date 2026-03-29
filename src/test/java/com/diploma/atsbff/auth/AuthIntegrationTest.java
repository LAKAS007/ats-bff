package com.diploma.atsbff.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void loginReturnsJwtAndProtectedEndpointAcceptsIt() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "demo@ats.local",
                      "password": "demo123"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isString())
            .andExpect(jsonPath("$.user.username").value("demo@ats.local"))
            .andReturn();

        String response = loginResult.getResponse().getContentAsString();
        String token = response.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(get("/api/auth/me")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.displayName").value("ATS Demo User"))
            .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void protectedEndpointRejectsUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
            .andExpect(status().isForbidden());
    }
}
