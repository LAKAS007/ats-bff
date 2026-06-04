package com.diploma.atsbff.demo;

import com.diploma.atsbff.analysis.AnalysisRequest;
import com.diploma.atsbff.assistant.AssistantMessage;
import com.diploma.atsbff.market.KlineResponse;
import com.diploma.atsbff.market.OptionTickerResponse;
import com.diploma.atsbff.market.OptionsChainResponse;
import com.diploma.atsbff.market.SpotTickerResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DemoDataService {

    private final ObjectMapper objectMapper;

    public DemoDataService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public SpotTickerResponse spotTicker(String asset) {
        String normalized = normalizeAsset(asset);
        if ("ETH".equals(normalized)) {
            return new SpotTickerResponse(
                "ETHUSDT",
                "3820.50",
                "3895.10",
                "3712.40",
                "845120.35",
                "3229412010.50",
                "0.0187"
            );
        }

        return new SpotTickerResponse(
            "BTCUSDT",
            "68420.75",
            "69780.20",
            "67110.00",
            "31245.92",
            "2138120450.30",
            "0.0124"
        );
    }

    public List<KlineResponse> klines(String asset, int requestedLimit) {
        String normalized = normalizeAsset(asset);
        double base = "ETH".equals(normalized) ? 3740.0 : 67200.0;
        int limit = Math.max(24, Math.min(requestedLimit, 120));
        long now = Instant.now().toEpochMilli();
        long hourMillis = 60L * 60L * 1000L;

        List<KlineResponse> result = new ArrayList<>();
        for (int i = limit - 1; i >= 0; i--) {
            double trend = (limit - i) * ("ETH".equals(normalized) ? 4.2 : 18.5);
            double wave = Math.sin((limit - i) / 4.0) * ("ETH".equals(normalized) ? 26.0 : 260.0);
            double close = base + trend + wave;
            double open = close - ("ETH".equals(normalized) ? 8.0 : 85.0);
            double high = Math.max(open, close) + ("ETH".equals(normalized) ? 18.0 : 180.0);
            double low = Math.min(open, close) - ("ETH".equals(normalized) ? 16.0 : 165.0);
            double volume = "ETH".equals(normalized) ? 12000 + i * 25.0 : 620 + i * 3.5;

            result.add(new KlineResponse(
                Long.toString(now - i * hourMillis),
                price(open),
                price(high),
                price(low),
                price(close),
                price(volume),
                price(volume * close)
            ));
        }
        return result;
    }

    public List<OptionsChainResponse> optionsChain(String asset) {
        String normalized = normalizeAsset(asset);
        String expiry = "ETH".equals(normalized) ? "26JUN26" : "26JUN26";
        double spot = "ETH".equals(normalized) ? 3820.0 : 68420.0;
        int[] strikes = "ETH".equals(normalized)
            ? new int[] {3600, 3700, 3800, 3900, 4000}
            : new int[] {64000, 66000, 68000, 70000, 72000};

        List<OptionTickerResponse> calls = new ArrayList<>();
        List<OptionTickerResponse> puts = new ArrayList<>();
        for (int strike : strikes) {
            calls.add(option(normalized, expiry, strike, "C", spot));
            puts.add(option(normalized, expiry, strike, "P", spot));
        }

        return List.of(new OptionsChainResponse(expiry, calls, puts));
    }

    public JsonNode analysisJson(AnalysisRequest request) {
        String asset = normalizeAsset(request.asset());
        BigDecimal nav = request.nav() == null ? new BigDecimal("100000") : request.nav();
        double positionSize = nav.multiply(new BigDecimal("0.80")).doubleValue();

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("approved", true);
        report.put("asset", asset);
        report.put("demo_mode", true);
        report.put("data_source", "demo-data");
        report.put("market_state", "ranging");
        report.put("stage_results", List.of(
                Map.of(
                    "stage_name", "Stage 1: Market State",
                    "passed", true,
                    "reason", "Demo: market is classified as ranging",
                    "data", Map.of("rsi", 54.8, "ema_12", 68120.0, "ema_26", 67880.0)
                ),
                Map.of(
                    "stage_name", "Stage 2: Volatility Edge",
                    "passed", true,
                    "reason", "Demo: implied volatility is above realized volatility",
                    "data", Map.of("iv_avg", 0.64, "rv_30d", 0.49, "iv_rv_spread", 0.15)
                ),
                Map.of(
                    "stage_name", "Stage 3: Liquidity",
                    "passed", true,
                    "reason", "Demo: option chain has enough open interest",
                    "data", Map.of("open_interest", 1450, "bid_ask_spread", 0.032)
                )
            ));
        report.put("selected_options", List.of(
                Map.of(
                    "symbol", asset + "-26JUN26-" + ("ETH".equals(asset) ? "4000" : "70000") + "-C",
                    "asset", asset,
                    "strike", "ETH".equals(asset) ? 4000.0 : 70000.0,
                    "option_type", "Call",
                    "mark_price", "ETH".equals(asset) ? 128.0 : 1520.0,
                    "mark_iv", 0.64,
                    "delta", 0.18
                )
            ));
        report.put("leverage", 2.0);
        report.put("position_size_usd", positionSize);
        report.put("rejection_reason", "");
        report.put("timestamp", Instant.now().toString());

        return objectMapper.valueToTree(report);
    }

    public String markdownReport(AnalysisRequest request) {
        String asset = normalizeAsset(request.asset());
        BigDecimal nav = request.nav() == null ? new BigDecimal("100000") : request.nav();
        String spot = spotTicker(asset).lastPrice();

        return """
            # ATS Demo Report: %s

            **Mode:** DEMO DATA
            **NAV:** %s USDC
            **Spot:** %s USDT

            ## Decision

            The demo strategy state is **APPROVED** for a limited volatility premium setup.

            ## Market Context

            - Market state: ranging
            - RSI: 54.8
            - Implied volatility is above realized volatility
            - Option liquidity is sufficient for the demonstration scenario

            ## Selected Structure

            - Instrument: %s-26JUN26-%s-C
            - Direction: short volatility / premium collection demo
            - Leverage: 2.0x
            - Position size: 80%% of NAV

            ## Risk Notes

            This report is generated from prepared demo-data. It is intended for a stable diploma defense flow
            when Bybit, Python ATS-service, or an LLM provider are unavailable.
            """.formatted(
            asset,
            nav.toPlainString(),
            spot,
            asset,
            "ETH".equals(asset) ? "4000" : "70000"
        );
    }

    public String assistantAnswer(List<AssistantMessage> messages) {
        String userText = messages == null || messages.isEmpty()
            ? ""
            : messages.get(messages.size() - 1).content();
        String asset = userText != null && userText.toUpperCase(Locale.ENGLISH).contains("ETH") ? "ETH" : "BTC";

        return """
            Demo-mode answer for %s.

            The system is using prepared market data, so the response is stable for the defense demo.
            Current demo context: ranging market, positive IV/RV spread, sufficient option liquidity,
            and an approved limited-risk volatility premium setup. For a real decision, switch DEMO_MODE=false
            and run the Python ATS-service, Bybit access, and Ollama/OpenRouter provider.
            """.formatted(asset);
    }

    private OptionTickerResponse option(String asset, String expiry, int strike, String type, double spot) {
        double distance = Math.abs(strike - spot) / spot;
        double markIv = 0.58 + distance;
        double delta = "C".equals(type)
            ? Math.max(0.08, 0.28 - distance)
            : -Math.max(0.08, 0.28 - distance);
        double markPrice = Math.max(12.0, spot * (0.018 + distance / 2.0));
        String symbol = "%s-%s-%d-%s".formatted(asset, expiry, strike, type);

        return new OptionTickerResponse(
            symbol,
            price(markPrice * 0.98),
            price(markPrice * 1.02),
            price(markPrice),
            price(markPrice * 1.15),
            price(markPrice * 0.72),
            price(markPrice),
            price(spot),
            String.format(Locale.ENGLISH, "%.4f", markIv),
            price(spot),
            price(900 + strike % 1000),
            price(markPrice * 10000),
            price(180 + strike % 100),
            String.format(Locale.ENGLISH, "%.4f", delta),
            "0.0001",
            "8.5000",
            "-4.2000",
            "0.0120"
        );
    }

    private String normalizeAsset(String asset) {
        String normalized = asset == null ? "BTC" : asset.trim().toUpperCase(Locale.ENGLISH);
        if (normalized.endsWith("USDT")) {
            return normalized.substring(0, normalized.length() - 4);
        }
        return normalized.isBlank() ? "BTC" : normalized;
    }

    private String price(double value) {
        return String.format(Locale.ENGLISH, "%.2f", value);
    }
}
