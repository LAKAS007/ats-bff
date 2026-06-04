package com.diploma.atsbff.demo;

import static org.assertj.core.api.Assertions.assertThat;

import com.diploma.atsbff.analysis.AnalysisRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class DemoDataServiceTest {

    private final DemoDataService demoDataService = new DemoDataService(new ObjectMapper());

    @Test
    void demoAnalysisMatchesFiveStageEngineAndControlledRiskAllocation() {
        AnalysisRequest request = new AnalysisRequest("BTC", new BigDecimal("100000"), "demo");

        JsonNode report = demoDataService.analysisJson(request);

        JsonNode stageResults = report.path("stage_results");
        assertThat(stageResults.size()).isEqualTo(5);
        assertThat(stageNames(stageResults)).containsExactly(
            "Stage 1: Market State",
            "Stage 2: Volatility Edge",
            "Stage 3: Hourly Filter",
            "Stage 4: Options Selection",
            "Stage 5: Decision Engine"
        );
        assertThat(report.path("position_size_usd").asDouble()).isEqualTo(16000.0);
        assertThat(stageResults.get(4).path("data").path("allocation_nav_pct").asInt()).isEqualTo(16);
        assertThat(demoDataService.markdownReport(request)).contains("Position size: 16% of NAV");
    }

    private static List<String> stageNames(JsonNode stageResults) {
        List<String> names = new ArrayList<>();
        stageResults.forEach(stage -> names.add(stage.path("stage_name").asText()));
        return names;
    }
}
