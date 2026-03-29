package com.diploma.atsbff.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analysis")
public class AnalysisController {

    private final PythonAnalyticsService pythonAnalyticsService;

    public AnalysisController(PythonAnalyticsService pythonAnalyticsService) {
        this.pythonAnalyticsService = pythonAnalyticsService;
    }

    @PostMapping("/run")
    public JsonNode run(@Valid @RequestBody AnalysisRequest request) {
        return pythonAnalyticsService.runAnalysis(request);
    }

    @PostMapping(value = "/markdown", produces = MediaType.TEXT_PLAIN_VALUE)
    public String markdown(@Valid @RequestBody AnalysisRequest request) {
        return pythonAnalyticsService.runMarkdownAnalysis(request);
    }
}
