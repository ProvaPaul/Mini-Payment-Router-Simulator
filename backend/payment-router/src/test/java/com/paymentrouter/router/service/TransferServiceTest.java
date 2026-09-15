package com.paymentrouter.router.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.ResourceAccessException;

import com.paymentrouter.router.client.DfspAAdapter;
import com.paymentrouter.router.client.DfspBAdapter;
import com.paymentrouter.router.client.DfspTransferRequest;
import com.paymentrouter.router.client.DfspTransferResult;
import com.paymentrouter.router.dto.TransferRequest;
import com.paymentrouter.router.dto.TransferResponse;
import com.paymentrouter.router.entity.Provider;
import com.paymentrouter.router.entity.ProviderStatus;
import com.paymentrouter.router.entity.Transaction;
import com.paymentrouter.router.entity.TransactionStatus;
import com.paymentrouter.router.exception.DfspCommunicationException;
import com.paymentrouter.router.exception.InvalidPaymentRequestException;
import com.paymentrouter.router.repository.TransactionRepository;
import com.paymentrouter.router.strategy.DfspAStrategy;
import com.paymentrouter.router.strategy.DfspBStrategy;
import com.paymentrouter.router.strategy.DfspStrategyFactory;

/**
 * Unit test for the transfer flow with the real strategies and factory.
 * Validation, adapters (HTTP) and the repository (database) are mocked.
 */
@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    private final Provider dfspA = provider("DFSP_A", "1.00", "http://dfsp-a.test");
    private final Provider dfspB = provider("DFSP_B", "1.50", "http://dfsp-b.test");

    @Mock
    private PaymentRequestValidator paymentRequestValidator;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private DfspAAdapter dfspAAdapter;

    @Mock
    private DfspBAdapter dfspBAdapter;

    private TransferService transferService;

    @BeforeEach
    void setUp() {
        DfspStrategyFactory factory = new DfspStrategyFactory(
                List.of(new DfspAStrategy(dfspAAdapter), new DfspBStrategy(dfspBAdapter)));
        transferService = new TransferService(paymentRequestValidator, factory, transactionRepository);
    }

    @Test
    void routesToDestinationDfspAndSavesSuccessfulTransaction() {
        when(paymentRequestValidator.validate("DFSP_A", "DFSP_B")).thenReturn(new PaymentProviders(dfspA, dfspB));
        when(dfspBAdapter.transfer(eq("http://dfsp-b.test"), any()))
                .thenReturn(new DfspTransferResult(true, "B-PAY-000123", "ACCEPTED"));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransferResponse response = transferService.executeTransfer(
                new TransferRequest("DFSP_A", "DFSP_B", new BigDecimal("1000")));

        ArgumentCaptor<DfspTransferRequest> sent = ArgumentCaptor.forClass(DfspTransferRequest.class);
        verify(dfspBAdapter).transfer(eq("http://dfsp-b.test"), sent.capture());
        verifyNoInteractions(dfspAAdapter);
        assertThat(sent.getValue().transactionId()).isEqualTo(response.transactionId());
        assertThat(sent.getValue().sourceProviderCode()).isEqualTo("DFSP_A");
        assertThat(sent.getValue().amount()).isEqualTo(new BigDecimal("1000.00"));
        assertThat(sent.getValue().feeAmount()).isEqualTo(new BigDecimal("15.00"));

        ArgumentCaptor<Transaction> saved = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(saved.capture());
        Transaction transaction = saved.getValue();
        assertThat(transaction.getTransactionId()).isEqualTo(response.transactionId());
        assertThat(transaction.getSourceProvider()).isSameAs(dfspA);
        assertThat(transaction.getDestinationProvider()).isSameAs(dfspB);
        assertThat(transaction.getAmount()).isEqualTo(new BigDecimal("1000.00"));
        assertThat(transaction.getFeePercentage()).isEqualTo(new BigDecimal("1.50"));
        assertThat(transaction.getFeeAmount()).isEqualTo(new BigDecimal("15.00"));
        assertThat(transaction.getTotalAmount()).isEqualTo(new BigDecimal("1015.00"));
        assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.SUCCESS);

        assertThat(response.status()).isEqualTo(TransactionStatus.SUCCESS);
        assertThat(response.message()).isEqualTo("ACCEPTED (ref B-PAY-000123)");
    }

    @Test
    void savesFailedTransactionWhenDfspRejects() {
        when(paymentRequestValidator.validate("DFSP_B", "DFSP_A")).thenReturn(new PaymentProviders(dfspB, dfspA));
        when(dfspAAdapter.transfer(eq("http://dfsp-a.test"), any()))
                .thenReturn(new DfspTransferResult(false, "A-TXN-0002", "Amount exceeds DFSP-A limit of 50000.00"));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransferResponse response = transferService.executeTransfer(
                new TransferRequest("DFSP_B", "DFSP_A", new BigDecimal("60000")));

        verifyNoInteractions(dfspBAdapter);
        assertThat(response.status()).isEqualTo(TransactionStatus.FAILED);
        assertThat(response.message()).isEqualTo("Amount exceeds DFSP-A limit of 50000.00 (ref A-TXN-0002)");
        assertThat(response.feePercentage()).isEqualTo(new BigDecimal("1.00"));
        assertThat(response.feeAmount()).isEqualTo(new BigDecimal("600.00"));
        assertThat(response.totalAmount()).isEqualTo(new BigDecimal("60600.00"));
    }

    @Test
    void savesFailedTransactionWhenDfspIsUnavailable() {
        when(paymentRequestValidator.validate("DFSP_A", "DFSP_B")).thenReturn(new PaymentProviders(dfspA, dfspB));
        when(dfspBAdapter.transfer(eq("http://dfsp-b.test"), any()))
                .thenThrow(new DfspCommunicationException("DFSP-B request failed: Connection refused"));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransferResponse response = transferService.executeTransfer(
                new TransferRequest("DFSP_A", "DFSP_B", new BigDecimal("1000")));

        ArgumentCaptor<Transaction> saved = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(TransactionStatus.FAILED);
        assertThat(response.status()).isEqualTo(TransactionStatus.FAILED);
        assertThat(response.message()).isEqualTo(TransferService.DFSP_UNAVAILABLE_MESSAGE);
    }

    @Test
    void savesFailedTransactionWithTimeoutMessageWhenDfspTimesOut() {
        when(paymentRequestValidator.validate("DFSP_A", "DFSP_B")).thenReturn(new PaymentProviders(dfspA, dfspB));
        // Same exception chain the adapter produces when RestClient's read timeout expires.
        DfspCommunicationException timeout = new DfspCommunicationException(
                "DFSP-B request failed: Read timed out",
                new ResourceAccessException("I/O error", new SocketTimeoutException("Read timed out")));
        when(dfspBAdapter.transfer(eq("http://dfsp-b.test"), any())).thenThrow(timeout);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransferResponse response = transferService.executeTransfer(
                new TransferRequest("DFSP_A", "DFSP_B", new BigDecimal("1000")));

        assertThat(response.status()).isEqualTo(TransactionStatus.FAILED);
        assertThat(response.message()).isEqualTo(TransferService.DFSP_TIMEOUT_MESSAGE);
    }

    @Test
    void doesNotCallDfspOrSaveWhenValidationFails() {
        when(paymentRequestValidator.validate("DFSP_A", "DFSP_A"))
                .thenThrow(new InvalidPaymentRequestException("Source and destination provider cannot be the same"));

        assertThatThrownBy(() -> transferService.executeTransfer(
                new TransferRequest("DFSP_A", "DFSP_A", new BigDecimal("1000"))))
                .isInstanceOf(InvalidPaymentRequestException.class);

        verifyNoInteractions(dfspAAdapter, dfspBAdapter, transactionRepository);
    }

    private static Provider provider(String code, String feePercentage, String baseUrl) {
        return new Provider(code, code, baseUrl, new BigDecimal(feePercentage), ProviderStatus.ACTIVE);
    }
}
