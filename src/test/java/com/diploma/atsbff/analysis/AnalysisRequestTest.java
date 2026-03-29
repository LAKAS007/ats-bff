package com.diploma.atsbff.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class AnalysisRequestTest {

    @Test
    void appliesPythonCompatibleDefaults() {
        AnalysisRequest request = new AnalysisRequest(null, null);

        assertThat(request.asset()).isEqualTo("BTC");
        assertThat(request.nav()).isEqualByComparingTo(new BigDecimal("100000"));
    }

    @Test
    void normalizesAssetSymbol() {
        AnalysisRequest request = new AnalysisRequest(" eth ", BigDecimal.ONE);

        assertThat(request.asset()).isEqualTo("ETH");
        assertThat(request.nav()).isEqualByComparingTo(BigDecimal.ONE);
    }
}
