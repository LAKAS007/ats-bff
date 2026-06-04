package com.diploma.atsbff.analysis;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.Locale;

public record AnalysisRequest(
    @NotBlank String asset,
    @DecimalMin("0.01") BigDecimal nav,
    String mode
) {

    private static final BigDecimal DEFAULT_NAV = new BigDecimal("100000");

    public AnalysisRequest(String asset, BigDecimal nav) {
        this(asset, nav, "live");
    }

    public AnalysisRequest {
        asset = normalizeAsset(asset);
        nav = nav == null ? DEFAULT_NAV : nav;
        mode = normalizeMode(mode);
    }

    public boolean isDemoMode() {
        return "demo".equals(mode);
    }

    private static String normalizeAsset(String asset) {
        if (asset == null || asset.isBlank()) {
            return "BTC";
        }
        return asset.trim().toUpperCase(Locale.ENGLISH);
    }

    private static String normalizeMode(String mode) {
        if (mode == null || mode.isBlank()) {
            return "live";
        }
        return "demo".equalsIgnoreCase(mode.trim()) ? "demo" : "live";
    }
}
