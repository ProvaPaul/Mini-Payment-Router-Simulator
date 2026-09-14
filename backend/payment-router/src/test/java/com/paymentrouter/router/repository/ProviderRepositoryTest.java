package com.paymentrouter.router.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import com.paymentrouter.router.entity.Provider;
import com.paymentrouter.router.entity.ProviderStatus;

/**
 * Runs against the configured PostgreSQL database.
 * Every test runs in a transaction that is rolled back, so no data is left behind.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProviderRepositoryTest {

    @Autowired
    private ProviderRepository providerRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findByCodeReturnsProviderWhenCodeExists() {
        providerRepository.save(testProvider("TEST_DFSP_B", "1.50"));
        clearPersistenceContext();

        Optional<Provider> found = providerRepository.findByCode("TEST_DFSP_B");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Test TEST_DFSP_B");
        assertThat(found.get().getFeePercentage()).isEqualByComparingTo("1.50");
        assertThat(found.get().getStatus()).isEqualTo(ProviderStatus.ACTIVE);
        assertThat(found.get().getCreatedAt()).isNotNull();
    }

    @Test
    void findByCodeReturnsEmptyWhenCodeDoesNotExist() {
        Optional<Provider> found = providerRepository.findByCode("TEST_UNKNOWN");

        assertThat(found).isEmpty();
    }

    @Test
    void findByIdReturnsSavedProvider() {
        Provider saved = providerRepository.save(testProvider("TEST_DFSP_A", "1.00"));
        clearPersistenceContext();

        Optional<Provider> found = providerRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getCode()).isEqualTo("TEST_DFSP_A");
    }

    /** Forces the next read to query the database instead of returning the cached object. */
    private void clearPersistenceContext() {
        entityManager.flush();
        entityManager.clear();
    }

    private static Provider testProvider(String code, String feePercentage) {
        return new Provider(code, "Test " + code, "http://localhost:9999",
                new BigDecimal(feePercentage), ProviderStatus.ACTIVE);
    }
}
