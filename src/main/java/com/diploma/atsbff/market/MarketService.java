package com.diploma.atsbff.market;

import com.diploma.atsbff.common.ApiException;
import com.diploma.atsbff.config.AppProperties;
import com.diploma.atsbff.demo.DemoDataService;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class MarketService {

    private final RestClient restClient;
    private final AppProperties properties;
    private final DemoDataService demoDataService;

    public MarketService(RestClient.Builder builder, AppProperties properties, DemoDataService demoDataService) {
        this.restClient = builder.baseUrl(properties.getBybit().getBaseUrl()).build();
        this.properties = properties;
        this.demoDataService = demoDataService;
    }

    public SpotTickerResponse getSpotTicker(String asset) {
        if (properties.isDemoMode()) {
            return demoDataService.spotTicker(asset);
        }

        String symbol = normalizeSpotSymbol(asset);
        JsonNode root = restClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/v5/market/tickers")
                .queryParam("category", "spot")
                .queryParam("symbol", symbol)
                .build()
            )
            .retrieve()
            .body(JsonNode.class);

        JsonNode item = firstResult(root);
        return new SpotTickerResponse(
            item.path("symbol").asText(symbol),
            item.path("lastPrice").asText("0"),
            item.path("highPrice24h").asText("0"),
            item.path("lowPrice24h").asText("0"),
            item.path("volume24h").asText("0"),
            item.path("turnover24h").asText("0"),
            item.path("price24hPcnt").asText("0")
        );
    }

    public List<KlineResponse> getKlines(String asset, String interval, int limit) {
        if (properties.isDemoMode()) {
            return demoDataService.klines(asset, limit);
        }

        String symbol = normalizeSpotSymbol(asset);
        JsonNode root = restClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/v5/market/kline")
                .queryParam("category", "spot")
                .queryParam("symbol", symbol)
                .queryParam("interval", interval)
                .queryParam("limit", limit)
                .build()
            )
            .retrieve()
            .body(JsonNode.class);

        JsonNode listNode = root.path("result").path("list");
        if (!listNode.isArray()) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Bybit returned invalid kline payload");
        }

        List<KlineResponse> result = new ArrayList<>();
        for (JsonNode item : listNode) {
            result.add(new KlineResponse(
                item.path(0).asText(),
                item.path(1).asText("0"),
                item.path(2).asText("0"),
                item.path(3).asText("0"),
                item.path(4).asText("0"),
                item.path(5).asText("0"),
                item.path(6).asText("0")
            ));
        }
        return result;
    }

    public List<OptionsChainResponse> getOptionsChain(String asset, String expDate) {
        if (properties.isDemoMode()) {
            return demoDataService.optionsChain(asset);
        }

        String normalizedAsset = normalizeAsset(asset);
        JsonNode root = restClient.get()
            .uri(uriBuilder -> {
                var builder = uriBuilder
                    .path("/v5/market/tickers")
                    .queryParam("category", "option")
                    .queryParam("baseCoin", normalizedAsset);
                if (expDate != null && !expDate.isBlank()) {
                    builder.queryParam("expDate", expDate.toUpperCase(Locale.ENGLISH));
                }
                return builder.build();
            })
            .retrieve()
            .body(JsonNode.class);

        JsonNode listNode = root.path("result").path("list");
        if (!listNode.isArray()) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Bybit returned invalid options payload");
        }

        Map<String, ExpiryBucket> grouped = new LinkedHashMap<>();
        for (JsonNode item : listNode) {
            String symbol = item.path("symbol").asText();
            String[] parts = symbol.split("-");
            if (parts.length < 4) {
                continue;
            }

            String expiry = parts[1];
            String optionType = parts[3];
            ExpiryBucket bucket = grouped.computeIfAbsent(expiry, ignored -> new ExpiryBucket());
            OptionTickerResponse ticker = new OptionTickerResponse(
                symbol,
                item.path("bid1Price").asText("0"),
                item.path("ask1Price").asText("0"),
                item.path("lastPrice").asText("0"),
                item.path("highPrice24h").asText("0"),
                item.path("lowPrice24h").asText("0"),
                item.path("markPrice").asText("0"),
                item.path("indexPrice").asText("0"),
                item.path("markIv").asText("0"),
                item.path("underlyingPrice").asText("0"),
                item.path("openInterest").asText("0"),
                item.path("turnover24h").asText("0"),
                item.path("volume24h").asText("0"),
                item.path("delta").asText("0"),
                item.path("gamma").asText("0"),
                item.path("vega").asText("0"),
                item.path("theta").asText("0"),
                item.path("change24h").asText("0")
            );

            if ("C".equalsIgnoreCase(optionType)) {
                bucket.calls.add(ticker);
            } else if ("P".equalsIgnoreCase(optionType)) {
                bucket.puts.add(ticker);
            }
        }

        List<OptionsChainResponse> chains = grouped.entrySet()
            .stream()
            .sorted(Map.Entry.comparingByKey(Comparator.comparing(this::parseExpiryDate)))
            .map(entry -> {
                entry.getValue().calls.sort(Comparator.comparingDouble(opt -> extractStrike(opt.symbol())));
                entry.getValue().puts.sort(Comparator.comparingDouble(opt -> extractStrike(opt.symbol())));
                return new OptionsChainResponse(entry.getKey(), entry.getValue().calls, entry.getValue().puts);
            })
            .toList();

        return chains;
    }

    private JsonNode firstResult(JsonNode root) {
        JsonNode listNode = root.path("result").path("list");
        if (!listNode.isArray() || listNode.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Bybit returned no market data");
        }
        return listNode.get(0);
    }

    private String normalizeSpotSymbol(String asset) {
        String normalized = normalizeAsset(asset);
        return normalized.endsWith("USDT") ? normalized : normalized + "USDT";
    }

    private String normalizeAsset(String asset) {
        String normalized = asset == null ? "" : asset.trim().toUpperCase(Locale.ENGLISH);
        if (normalized.endsWith("USDT")) {
            return normalized;
        }
        if (normalized.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Asset must not be blank");
        }
        return normalized;
    }

    private LocalDate parseExpiryDate(String expiry) {
        String normalized = expiry.toUpperCase(Locale.ENGLISH);
        int monthStart = -1;
        for (int i = 0; i < normalized.length(); i++) {
            if (Character.isLetter(normalized.charAt(i))) {
                monthStart = i;
                break;
            }
        }

        if (monthStart <= 0 || normalized.length() < monthStart + 5) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Unsupported options expiry format: " + expiry);
        }

        int day = Integer.parseInt(normalized.substring(0, monthStart));
        String monthString = normalized.substring(monthStart, monthStart + 3);
        int year = 2000 + Integer.parseInt(normalized.substring(monthStart + 3));

        int month = switch (monthString) {
            case "JAN" -> 1;
            case "FEB" -> 2;
            case "MAR" -> 3;
            case "APR" -> 4;
            case "MAY" -> 5;
            case "JUN" -> 6;
            case "JUL" -> 7;
            case "AUG" -> 8;
            case "SEP" -> 9;
            case "OCT" -> 10;
            case "NOV" -> 11;
            case "DEC" -> 12;
            default -> throw new ApiException(HttpStatus.BAD_GATEWAY, "Unsupported options expiry month: " + expiry);
        };

        return LocalDate.of(year, month, day);
    }

    private double extractStrike(String symbol) {
        String[] parts = symbol.split("-");
        if (parts.length < 3) {
            return 0.0;
        }
        try {
            return Double.parseDouble(parts[2]);
        } catch (NumberFormatException ignored) {
            return 0.0;
        }
    }

    private static class ExpiryBucket {
        private final List<OptionTickerResponse> calls = new ArrayList<>();
        private final List<OptionTickerResponse> puts = new ArrayList<>();
    }
}
