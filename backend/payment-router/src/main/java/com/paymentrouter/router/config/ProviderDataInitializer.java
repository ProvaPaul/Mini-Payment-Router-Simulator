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
 * The values below are only the INITIAL configuration. At runtime the router
 * always reads provider configuration from the providers table.
 */
@Component
public class ProviderDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ProviderDataInitializer.class);

    private final ProviderRepository providerRepository;
    private final String dfspABaseUrl;
    private final String dfspBBaseUrl;

    public ProviderDataInitializer(
            ProviderRepository providerRepository,
            @Value("${dfsp.a.base-url}") String dfspABaseUrl,
            @Value("${dfsp.b.base-url}") String dfspBBaseUrl) {
        this.providerRepository = providerRepository;
        this.dfspABaseUrl = dfspABaseUrl;
        this.dfspBBaseUrl = dfspBBaseUrl;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedIfMissing("DFSP_A", "DFSP-A", dfspABaseUrl, new BigDecimal("1.00"));
        seedIfMissing("DFSP_B", "DFSP-B", dfspBBaseUrl, new BigDecimal("1.50"));
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
