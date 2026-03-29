package com.diploma.atsbff.market;

public record OptionTickerResponse(
    String symbol,
    String bid1Price,
    String ask1Price,
    String lastPrice,
    String highPrice24h,
    String lowPrice24h,
    String markPrice,
    String indexPrice,
    String markIv,
    String underlyingPrice,
    String openInterest,
    String turnover24h,
    String volume24h,
    String delta,
    String gamma,
    String vega,
    String theta,
    String change24h
) {
}
