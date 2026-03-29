package com.diploma.atsbff.market;

public record KlineResponse(
    String startTime,
    String openPrice,
    String highPrice,
    String lowPrice,
    String closePrice,
    String volume,
    String turnover
) {
}
