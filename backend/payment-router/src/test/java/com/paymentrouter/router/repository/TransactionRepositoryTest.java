package com.paymentrouter.router.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import com.paymentrouter.router.entity.Provider;
import com.paymentrouter.router.entity.ProviderStatus;
import com.paymentrouter.router.entity.Transaction;
import com.paymentrouter.router.entity.TransactionStatus;

/**
 * Runs against the configured PostgreSQL database.
 * Every test runs in a transaction that is rolled back, so no data is left behind.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TransactionRepositoryTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private ProviderRepository providerRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void saveStoresTransactionWithBothProvidersAndPricingSnapshot() {
        Provider source = providerRepository.save(testProvider("TEST_DFSP_A", "1.00"));
        Provider destination = providerRepository.save(testProvider("TEST_DFSP_B", "1.50"));
        UUID transactionId = UUID.randomUUID();

        Transaction saved = transactionRepository.save(new Transaction(
                transactionId,
                source,
                destination,
                new BigDecimal("1000.00"),
                new BigDecimal("1.50"),
                new BigDecimal("15.00"),
                new BigDecimal("1015.00"),
                TransactionStatus.SUCCESS));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();

        entityManager.flush();
        entityManager.clear();

        Transaction loaded = transactionRepository.findById(saved.getId()).orElseThrow();

        assertThat(loaded.getTransactionId()).isEqualTo(transactionId);
        assertThat(loaded.getSourceProvider().getCode()).isEqualTo("TEST_DFSP_A");
        assertThat(loaded.getDestinationProvider().getCode()).isEqualTo("TEST_DFSP_B");
        assertThat(loaded.getAmount()).isEqualByComparingTo("1000.00");
        assertThat(loaded.getFeePercentage()).isEqualByComparingTo("1.50");
        assertThat(loaded.getFeeAmount()).isEqualByComparingTo("15.00");
        assertThat(loaded.getTotalAmount()).isEqualByComparingTo("1015.00");
        assertThat(loaded.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
    }

    private static Provider testProvider(String code, String feePercentage) {
        return new Provider(code, "Test " + code, "http://localhost:9999",
                new BigDecimal(feePercentage), ProviderStatus.ACTIVE);
    }
}
