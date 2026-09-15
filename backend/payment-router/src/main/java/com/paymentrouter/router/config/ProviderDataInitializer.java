package com.paymentrouter.router.config;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.paymentrouter.router.entity.Provider;
import com.paymentrouter.router.entity.ProviderStatus;
import com.paymentrouter.router.repository.ProviderRepository;

/**
 * Inserts the initial provider configuration when the application starts.
 * <p>
 * A provider is inserted only if no provider with the same code exists.
 * Existing rows are never overwritten, so configuration changed later in the
 * database (for example a new fee percentage) survives application restarts.
 * <p>
 * The values come from application.properties (dfsp.*.base-url, dfsp.*.fee-percentage) and are
 * only the INITIAL configuration. At runtime the router
 * always reads provider configuration from the providers table.
 */
@Component
public class ProviderDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ProviderDataInitializer.class);

    private final ProviderRepository providerRepository;
    private final String dfspABaseUrl;
    private final String dfspBBaseUrl;
    private final BigDecimal dfspAFeePercentage;
    private final BigDecimal dfspBFeePercentage;

    public ProviderDataInitializer(
            ProviderRepository providerRepository,
            @Value("${dfsp.a.base-url}") String dfspABaseUrl,
            @Value("${dfsp.b.base-url}") String dfspBBaseUrl,
            @Value("${dfsp.a.fee-percentage}") BigDecimal dfspAFeePercentage,
            @Value("${dfsp.b.fee-percentage}") BigDecimal dfspBFeePercentage) {
        this.providerRepository = providerRepository;
        this.dfspABaseUrl = dfspABaseUrl;
        this.dfspBBaseUrl = dfspBBaseUrl;
        this.dfspAFeePercentage = dfspAFeePercentage;
        this.dfspBFeePercentage = dfspBFeePercentage;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedIfMissing("DFSP_A", "DFSP-A", dfspABaseUrl, dfspAFeePercentage);
        seedIfMissing("DFSP_B", "DFSP-B", dfspBBaseUrl, dfspBFeePercentage);
    }

    private void seedIfMissing(String code, String name, String baseUrl, BigDecimal feePercentage) {
        if (providerRepository.findByCode(code).isPresent()) {
            log.info("Provider {} already exists, keeping stored configuration", code);
            return;
        }

        providerRepository.save(new Provider(code, name, baseUrl, feePercentage, ProviderStatus.ACTIVE));
        log.info("Seeded provider {} (feePercentage={}, baseUrl={})", code, feePercentage, baseUrl);
    }
}
