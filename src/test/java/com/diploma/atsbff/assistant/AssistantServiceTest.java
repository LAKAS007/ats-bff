package com.diploma.atsbff.assistant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.diploma.atsbff.analysis.AnalysisRequest;
import com.diploma.atsbff.analysis.PythonAnalyticsService;
import com.diploma.atsbff.market.KlineResponse;
import com.diploma.atsbff.market.MarketService;
import com.diploma.atsbff.market.OptionsChainResponse;
import com.diploma.atsbff.market.OptionTickerResponse;
import com.diploma.atsbff.market.SpotTickerResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AssistantServiceTest {

    @Mock
    private MarketService marketService;

    @Mock
    private PythonAnalyticsService pythonAnalyticsService;

    @Mock
    private OllamaChatService ollamaChatService;

    @Test
    void queryUsesMarketToolsAndReturnsStructuredPayload() {
        AssistantService assistantService = new AssistantService(
            marketService,
            pythonAnalyticsService,
            ollamaChatService,
            new ObjectMapper()
        );

        when(marketService.getSpotTicker("BTC")).thenReturn(new SpotTickerResponse(
            "BTCUSDT",
            "80000",
            "82000",
            "78000",
            "1000",
            "80000000",
            "0.025"
        ));
        when(marketService.getKlines(eq("BTC"), eq("60"), eq(48))).thenReturn(List.of(
            new KlineResponse("1", "79000", "80000", "78500", "79500", "10", "100"),
            new KlineResponse("2", "79500", "80500", "79000", "80200", "12", "120")
        ));
        when(marketService.getOptionsChain(eq("BTC"), eq(null))).thenReturn(List.of(
            new OptionsChainResponse(
                "28MAR26",
                List.of(new OptionTickerResponse(
                    "BTC-28MAR26-90000-C",
                    "1",
                    "2",
                    "3",
                    "4",
                    "5",
                    "6",
                    "7",
                    "0.5",
                    "80000",
                    "10",
                    "11",
                    "12",
                    "0.2",
                    "0.1",
                    "0.3",
                    "-0.1",
                    "0.01"
                )),
                List.of()
            )
        ));
        when(pythonAnalyticsService.runAnalysis(any(AnalysisRequest.class)))
            .thenReturn(new ObjectMapper().valueToTree(
                java.util.Map.of(
                    "approved", true,
                    "market_state", "ranging",
                    "leverage", 2.0,
                    "rejection_reason", ""
                )
            ));
        when(ollamaChatService.chat(anyList(), any())).thenReturn("BTC выглядит устойчиво.");

        AssistantQueryResponse response = assistantService.query(new AssistantQueryRequest(
            List.of(new AssistantMessage("user", "Проанализируй BTC, тренд и опционы")),
            true,
            "llama3.1:8b"
        ));

        assertThat(response.asset()).isEqualTo("BTC");
        assertThat(response.content()).contains("устойчиво");
        assertThat(response.toolsUsed()).contains("market.spot", "market.klines", "market.options", "analysis.run");
        assertThat(response.marketSnapshot()).containsKeys("spot", "trend", "options");
        assertThat(response.analysis()).containsEntry("approved", true);
    }
}
