package com.diploma.atsbff.market;

import java.util.List;

public record OptionsChainResponse(
    String expiry,
    List<OptionTickerResponse> calls,
    List<OptionTickerResponse> puts
) {
}
