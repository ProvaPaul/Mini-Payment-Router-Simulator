package com.paymentrouter.router.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import com.paymentrouter.router.client.DfspAAdapter;
import com.paymentrouter.router.client.DfspBAdapter;
import com.paymentrouter.router.client.DfspTransferResult;
import com.paymentrouter.router.dto.TransferRequest;
import com.paymentrouter.router.dto.TransferResponse;
import com.paymentrouter.router.entity.Transaction;
import com.paymentrouter.router.entity.TransactionStatus;
import com.paymentrouter.router.exception.DfspCommunicationException;
import com.paymentrouter.router.exception.InvalidPaymentRequestException;
import com.paymentrouter.router.repository.ProviderRepository;
import com.paymentrouter.router.repository.TransactionRepository;

import jakarta.persistence.EntityManager;

/**
 * Proves that transfer attempts are persisted in PostgreSQL with their provider foreign keys,
 * pricing snapshot, status and timestamp.
 * <p>
 * Full Spring context and the real database; only the DFSP adapters (HTTP) are mocked.
 * Each test runs in a transaction that is rolled back, so no rows are left behind.
 */
@SpringBootTest
@Transactional
class TransferPersistenceIntegrationTest {

    @Autowired
    private TransferService transferService;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private ProviderRepository providerRepository;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private DfspAAdapter dfspAAdapter;

    @MockitoBean
    private DfspBAdapter dfspBAdapter;

    @Test
    void persistsSuccessfulTransfer() {
        when(dfspBAdapter.transfer(any(), any())).thenReturn(new DfspTransferResult(true, "B-PAY-TEST0001", "ACCEPTED"));

        TransferResponse response = transferService.executeTransfer(
                new TransferRequest("DFSP_A", "DFSP_B", new BigDecimal("1000")));

        Transaction saved = reloadFromDatabase(response.transactionId());
        BigDecimal currentFee = providerRepository.findByCode("DFSP_B").orElseThrow().getFeePercentage();

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTransactionId()).isEqualTo(response.transactionId());
        assertThat(saved.getSourceProvider().getCode()).isEqualTo("DFSP_A");
        assertThat(saved.getDestinationProvider().getCode()).isEqualTo("DFSP_B");
        assertThat(saved.getAmount()).isEqualByComparingTo("1000.00");
        assertThat(saved.getFeePercentage()).isEqualByComparingTo(currentFee);
        assertThat(saved.getFeeAmount()).isEqualByComparingTo(expectedFee("1000.00", currentFee));
        assertThat(saved.getTotalAmount()).isEqualByComparingTo(saved.getAmount().add(saved.getFeeAmount()));
        assertThat(saved.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void persistsTransferRejectedByDfspAsFailed() {
        when(dfspAAdapter.transfer(any(), any()))
                .thenReturn(new DfspTransferResult(false, "A-TXN-TEST0002", "Amount exceeds DFSP-A limit of 50000.00"));

        TransferResponse response = transferService.executeTransfer(
                new TransferRequest("DFSP_B", "DFSP_A", new BigDecimal("60000")));

        Transaction saved = reloadFromDatabase(response.transactionId());

        assertThat(saved.getSourceProvider().getCode()).isEqualTo("DFSP_B");
        assertThat(saved.getDestinationProvider().getCode()).isEqualTo("DFSP_A");
        assertThat(saved.getAmount()).isEqualByComparingTo("60000.00");
        assertThat(saved.getStatus()).isEqualTo(TransactionStatus.FAILED);
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void persistsTransferToUnavailableDfspAsFailed() {
        when(dfspBAdapter.transfer(any(), any()))
                .thenThrow(new DfspCommunicationException("DFSP-B request failed: Connection refused"));

        TransferResponse response = transferService.executeTransfer(
                new TransferRequest("DFSP_A", "DFSP_B", new BigDecimal("1000")));

        Transaction saved = reloadFromDatabase(response.transactionId());

        assertThat(saved.getStatus()).isEqualTo(TransactionStatus.FAILED);
        assertThat(response.message()).isEqualTo("Destination DFSP is unavailable");
    }

    @Test
    void persistsNothingWhenValidationFails() {
        long rowsBefore = transactionRepository.count();

        assertThatThrownBy(() -> transferService.executeTransfer(
                new TransferRequest("DFSP_A", "DFSP_X", new BigDecimal("1000"))))
                .isInstanceOf(InvalidPaymentRequestException.class);

        assertThat(transactionRepository.count()).isEqualTo(rowsBefore);
    }

    @Test
    void persistsTransferWhenSourceAndDestinationAreTheSameProvider() {
        when(dfspAAdapter.transfer(any(), any()))
                .thenReturn(new DfspTransferResult(true, "A-TXN-TEST0003", "Transfer completed"));

        TransferResponse response = transferService.executeTransfer(
                new TransferRequest("DFSP_A", "DFSP_A", new BigDecimal("1000")));

        Transaction saved = reloadFromDatabase(response.transactionId());

        // Both foreign keys point at the same providers row; the schema already allows this
        // since source_provider_id and destination_provider_id are independent columns.
        assertThat(saved.getSourceProvider().getCode()).isEqualTo("DFSP_A");
        assertThat(saved.getDestinationProvider().getCode()).isEqualTo("DFSP_A");
        assertThat(saved.getSourceProvider().getId()).isEqualTo(saved.getDestinationProvider().getId());
        assertThat(saved.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
    }

    /**
     * Writes pending SQL, empties Hibernate's first-level cache and reads the row again,
     * so the assertions check what is really stored in PostgreSQL.
     */
    private Transaction reloadFromDatabase(UUID transactionId) {
        entityManager.flush();
        entityManager.clear();
        return transactionRepository.findAll().stream()
                .filter(transaction -> transaction.getTransactionId().equals(transactionId))
                .findFirst()
                .orElseThrow();
    }

    private static BigDecimal expectedFee(String amount, BigDecimal feePercentage) {
        return new BigDecimal(amount).multiply(feePercentage).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }
}
