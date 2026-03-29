package com.diploma.atsbff.analysis;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.Locale;

public record AnalysisRequest(
    @NotBlank String asset,
    @DecimalMin("0.01") BigDecimal nav
) {

    private static final BigDecimal DEFAULT_NAV = new BigDecimal("100000");

    public AnalysisRequest {
        asset = normalizeAsset(asset);
        nav = nav == null ? DEFAULT_NAV : nav;
    }

    private static String normalizeAsset(String asset) {
        if (asset == null || asset.isBlank()) {
            return "BTC";
        }
        return asset.trim().toUpperCase(Locale.ENGLISH);
    }
}
