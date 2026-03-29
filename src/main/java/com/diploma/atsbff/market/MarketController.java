package com.diploma.atsbff.market;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/market")
public class MarketController {

    private final MarketService marketService;

    public MarketController(MarketService marketService) {
        this.marketService = marketService;
    }

    @GetMapping("/spot/{asset}")
    public SpotTickerResponse spot(@PathVariable String asset) {
        return marketService.getSpotTicker(asset);
    }

    @GetMapping("/klines/{asset}")
    public List<KlineResponse> klines(
        @PathVariable String asset,
        @RequestParam(defaultValue = "60") String interval,
        @RequestParam(defaultValue = "100") int limit
    ) {
        return marketService.getKlines(asset, interval, limit);
    }

    @GetMapping("/options/{asset}")
    public List<OptionsChainResponse> options(
        @PathVariable String asset,
        @RequestParam(required = false) String expDate
    ) {
        return marketService.getOptionsChain(asset, expDate);
    }
}
