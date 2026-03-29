package com.diploma.atsbff.market;

public record SpotTickerResponse(
    String symbol,
    String lastPrice,
    String highPrice24h,
    String lowPrice24h,
    String volume24h,
    String turnover24h,
    String price24hPcnt
) {
}
