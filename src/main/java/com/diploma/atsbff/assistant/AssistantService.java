package com.diploma.atsbff.assistant;

import com.diploma.atsbff.analysis.AnalysisRequest;
import com.diploma.atsbff.analysis.PythonAnalyticsService;
import com.diploma.atsbff.market.KlineResponse;
import com.diploma.atsbff.market.MarketService;
import com.diploma.atsbff.market.OptionsChainResponse;
import com.diploma.atsbff.market.OptionTickerResponse;
import com.diploma.atsbff.market.SpotTickerResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class AssistantService {

    private static final String SYSTEM_PROMPT = """
        You are the ATS market assistant.
        Respond in Russian.
        Use only the market facts and analysis provided by the backend.
        If some metric is missing, say that it is unavailable.
        Keep the answer concise, practical, and numeric when data is present.
        """;

    private final MarketService marketService;
    private final PythonAnalyticsService pythonAnalyticsService;
    private final OllamaChatService ollamaChatService;
    private final ObjectMapper objectMapper;

    public AssistantService(
        MarketService marketService,
        PythonAnalyticsService pythonAnalyticsService,
        OllamaChatService ollamaChatService,
        ObjectMapper objectMapper
    ) {
        this.marketService = marketService;
        this.pythonAnalyticsService = pythonAnalyticsService;
        this.ollamaChatService = ollamaChatService;
        this.objectMapper = objectMapper;
    }

    public AssistantQueryResponse query(AssistantQueryRequest request) {
        List<AssistantMessage> sanitizedMessages = request.messages()
            .stream()
            .filter(message -> message != null && message.content() != null && !message.content().isBlank())
            .toList();

        int latestUserIndex = findLatestUserMessageIndex(sanitizedMessages);
        AssistantMessage latestUserMessage = sanitizedMessages.get(latestUserIndex);

        String latestPrompt = latestUserMessage.content().trim();
        String asset = detectAsset(sanitizedMessages).orElse("BTC");
        boolean usedDefaultAsset = detectAsset(sanitizedMessages).isEmpty();
        boolean includeMarketContext = request.includeMarketContext() == null || request.includeMarketContext();

        List<String> toolsUsed = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        Map<String, Object> marketSnapshot = new LinkedHashMap<>();
        Map<String, Object> analysisSnapshot = null;

        StringBuilder context = new StringBuilder();
        context.append("Backend context:\n");
        context.append("- Asset: ").append(asset).append("\n");
        if (usedDefaultAsset) {
            context.append("- Asset fallback: asset was not explicit, defaulted to BTC.\n");
        }

        if (includeMarketContext) {
            try {
                SpotTickerResponse spot = marketService.getSpotTicker(asset);
                toolsUsed.add("market.spot");
                marketSnapshot.put("spot", spot);
                context.append("- Spot: ").append(spot.lastPrice()).append(" USDT\n");
                context.append("- 24h change: ").append(spot.price24hPcnt()).append("\n");
            } catch (Exception exception) {
                warnings.add("Spot data unavailable");
            }

            if (requiresTrendData(latestPrompt)) {
                try {
                    List<KlineResponse> klines = marketService.getKlines(asset, "60", 48);
                    toolsUsed.add("market.klines");
                    Map<String, Object> trendSummary = summarizeTrend(klines);
                    marketSnapshot.put("trend", trendSummary);
                    context.append("- Trend summary: ").append(trendSummary).append("\n");
                } catch (Exception exception) {
                    warnings.add("Kline data unavailable");
                }
            }

            if (requiresOptionsData(latestPrompt)) {
                try {
                    List<OptionsChainResponse> chains = marketService.getOptionsChain(asset, null);
                    toolsUsed.add("market.options");
                    Map<String, Object> optionsSummary = summarizeOptions(chains);
                    marketSnapshot.put("options", optionsSummary);
                    context.append("- Options summary: ").append(optionsSummary).append("\n");
                } catch (Exception exception) {
                    warnings.add("Options data unavailable");
                }
            }

            if (requiresAnalysis(latestPrompt)) {
                try {
                    JsonNode analysis = pythonAnalyticsService.runAnalysis(
                        new AnalysisRequest(asset, new BigDecimal("100000"))
                    );
                    toolsUsed.add("analysis.run");
                    analysisSnapshot = objectMapper.convertValue(
                        analysis,
                        new TypeReference<>() { }
                    );
                    context.append(renderAnalysisSummary(analysisSnapshot));
                } catch (Exception exception) {
                    warnings.add("Python analysis unavailable");
                }
            }
        }

        if (!warnings.isEmpty()) {
            marketSnapshot.put("warnings", warnings);
            context.append("- Warnings: ").append(warnings).append("\n");
        }

        List<AssistantMessage> promptMessages = new ArrayList<>();
        promptMessages.add(new AssistantMessage("system", SYSTEM_PROMPT));

        int historyStart = Math.max(0, latestUserIndex - 6);
        for (int i = historyStart; i < latestUserIndex; i++) {
            AssistantMessage message = sanitizedMessages.get(i);
            promptMessages.add(normalizeRole(message));
        }

        promptMessages.add(
            new AssistantMessage(
                "user",
                latestPrompt + "\n\n" + context + "\nUse the backend context above and avoid inventing values."
            )
        );

        String answer = ollamaChatService.chat(promptMessages, request.model());
        return new AssistantQueryResponse(answer, asset, toolsUsed, marketSnapshot, analysisSnapshot);
    }

    private int findLatestUserMessageIndex(List<AssistantMessage> messages) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            if ("user".equalsIgnoreCase(messages.get(i).role())) {
                return i;
            }
        }
        throw new IllegalArgumentException("At least one user message is required");
    }

    private Optional<String> detectAsset(List<AssistantMessage> messages) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            String upper = messages.get(i).content().toUpperCase(Locale.ENGLISH);
            if (upper.contains("BTC")) {
                return Optional.of("BTC");
            }
            if (upper.contains("ETH")) {
                return Optional.of("ETH");
            }
        }
        return Optional.empty();
    }

    private AssistantMessage normalizeRole(AssistantMessage message) {
        String role = "assistant".equalsIgnoreCase(message.role()) ? "assistant" : "user";
        return new AssistantMessage(role, message.content());
    }

    private boolean requiresTrendData(String prompt) {
        String lower = prompt.toLowerCase(Locale.ENGLISH);
        return containsAny(lower, "trend", "chart", "candl", "momentum", "граф", "свеч", "тренд", "движ");
    }

    private boolean requiresOptionsData(String prompt) {
        String lower = prompt.toLowerCase(Locale.ENGLISH);
        return containsAny(lower, "option", "iv", "delta", "gamma", "vega", "strike", "expiry", "опцион", "волат");
    }

    private boolean requiresAnalysis(String prompt) {
        String lower = prompt.toLowerCase(Locale.ENGLISH);
        return containsAny(
            lower,
            "analysis",
            "report",
            "signal",
            "recommend",
            "strategy",
            "should",
            "buy",
            "sell",
            "анализ",
            "отчет",
            "сигнал",
            "рекомен",
            "стратег",
            "покуп",
            "прод"
        );
    }

    private boolean containsAny(String source, String... candidates) {
        for (String candidate : candidates) {
            if (source.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    private Map<String, Object> summarizeTrend(List<KlineResponse> klines) {
        List<KlineResponse> chronological = new ArrayList<>(klines);
        chronological.sort((left, right) -> left.startTime().compareTo(right.startTime()));

        KlineResponse first = chronological.get(0);
        KlineResponse last = chronological.get(chronological.size() - 1);

        double open = parseDouble(first.closePrice());
        double close = parseDouble(last.closePrice());
        double high = chronological.stream().mapToDouble(kline -> parseDouble(kline.highPrice())).max().orElse(close);
        double low = chronological.stream().mapToDouble(kline -> parseDouble(kline.lowPrice())).min().orElse(close);
        double changePercent = open == 0 ? 0 : ((close - open) / open) * 100.0;

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("periodHours", chronological.size());
        summary.put("open", open);
        summary.put("close", close);
        summary.put("high", high);
        summary.put("low", low);
        summary.put("changePercent", changePercent);
        return summary;
    }

    private Map<String, Object> summarizeOptions(List<OptionsChainResponse> chains) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("expiries", chains.size());

        if (chains.isEmpty()) {
            return summary;
        }

        OptionsChainResponse nearest = chains.get(0);
        summary.put("nearestExpiry", nearest.expiry());
        summary.put("callCount", nearest.calls().size());
        summary.put("putCount", nearest.puts().size());
        summary.put("callAverageIv", averageIv(nearest.calls()));
        summary.put("putAverageIv", averageIv(nearest.puts()));
        return summary;
    }

    private String renderAnalysisSummary(Map<String, Object> analysis) {
        Object approved = analysis.get("approved");
        Object marketState = analysis.get("market_state");
        Object leverage = analysis.get("leverage");
        Object rejectionReason = analysis.get("rejection_reason");

        return """
            - Python analysis summary:
              approved=%s
              market_state=%s
              leverage=%s
              rejection_reason=%s
            """.formatted(approved, marketState, leverage, rejectionReason);
    }

    private double averageIv(List<OptionTickerResponse> tickers) {
        return tickers.stream()
            .mapToDouble(ticker -> parseDouble(ticker.markIv()))
            .average()
            .orElse(0.0);
    }

    private double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException exception) {
            return 0.0;
        }
    }
}
