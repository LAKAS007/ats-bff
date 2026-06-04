package com.diploma.atsbff.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "app.demo-mode=true")
@AutoConfigureMockMvc
class HealthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void dependenciesEndpointReturnsDemoStatusesInDemoMode() throws Exception {
        mockMvc.perform(get("/api/health/dependencies"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("healthy"))
            .andExpect(jsonPath("$.demoMode").value(true))
            .andExpect(jsonPath("$.dependencies.bff").value("ok"))
            .andExpect(jsonPath("$.dependencies.pythonAtsService").value("demo-data"))
            .andExpect(jsonPath("$.dependencies.bybit").value("demo-data"))
            .andExpect(jsonPath("$.dependencies.llm").value("demo-data"));
    }
}
