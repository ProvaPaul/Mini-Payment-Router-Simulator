package com.paymentrouter.router;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;
import com.paymentrouter.router.entity.Provider;
import com.paymentrouter.router.entity.Transaction;
import com.paymentrouter.router.entity.TransactionStatus;
import com.paymentrouter.router.repository.ProviderRepository;
import com.paymentrouter.router.repository.TransactionRepository;
import com.sun.net.httpserver.HttpServer;

import jakarta.persistence.EntityManager;

/**
 * End-to-end test of the router's public API with nothing mocked inside the router:
 * HTTP request → validation → service → strategy → adapter → real RestClient → DFSP over HTTP
 * → PostgreSQL → HTTP response, plus the log file.
 * <p>
 * DFSP-A and DFSP-B are replaced by two tiny in-process HTTP servers that speak their real
 * wire formats, so no other service has to be running. Each test runs in a transaction that
 * is rolled back, so the provider changes made here and the saved transactions are not kept.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PaymentApiIntegrationTest {

    private static FakeDfsp dfspA;
    private static FakeDfsp dfspB;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProviderRepository providerRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private Environment environment;

    @BeforeAll
    static void startFakeDfsps() throws IOException {
        dfspA = new FakeDfsp("/api/dfsp-a/transfers");
        dfspB = new FakeDfsp("/v1/payments/receive");
    }

    @AfterAll
    static void stopFakeDfsps() {
        dfspA.stop();
        dfspB.stop();
    }

    /** Known fees and base URLs pointing at the fake DFSPs (rolled back after each test). */
    @BeforeEach
    void pointProvidersAtFakeDfsps() {
        configureProvider("DFSP_A", "1.00", dfspA.baseUrl());
        configureProvider("DFSP_B", "1.50", dfspB.baseUrl());
        dfspA.reset("""
                {"status":"SUCCESS","referenceId":"A-TXN-IT000001","message":"Transfer completed"}
                """);
        dfspB.reset("""
                {"result":"ACCEPTED","paymentRef":"B-PAY-IT000001"}
                """);
    }

    // ---- 1. Valid quote ----

    @Test
    void validQuoteUsesDestinationFeeAndCallsNoDfsp() throws Exception {
        long rowsBefore = transactionRepository.count();

        postJson("/api/quotes", payment("DFSP_A", "DFSP_B", "1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceProviderCode").value("DFSP_A"))
                .andExpect(jsonPath("$.destinationProviderCode").value("DFSP_B"))
                .andExpect(jsonPath("$.amount").value(1000.00))
                .andExpect(jsonPath("$.feePercentage").value(1.50))
                .andExpect(jsonPath("$.feeAmount").value(15.00))
                .andExpect(jsonPath("$.totalAmount").value(1015.00));

        assertThat(dfspB.requestCount()).isZero();
        assertThat(transactionRepository.count()).isEqualTo(rowsBefore);
    }

    // ---- 2 and 4. Rejected requests ----

    @Test
    void invalidAmountIsRejectedBeforeAnyDfspCallOrSave() throws Exception {
        long rowsBefore = transactionRepository.count();

        postJson("/api/transfers", payment("DFSP_A", "DFSP_B", "-50"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.amount").value("amount must be greater than zero"));

        assertThat(dfspB.requestCount()).isZero();
        assertThat(transactionRepository.count()).isEqualTo(rowsBefore);
    }

    @Test
    void unknownProviderIsRejected() throws Exception {
        postJson("/api/quotes", payment("DFSP_A", "DFSP_X", "1000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Provider not found: DFSP_X"));
    }

    // ---- 5–7. Transfers in both directions, persisted ----

    @Test
    void transferAToBIsSentToDfspBInItsFormatAndPersisted() throws Exception {
        MvcResult result = postJson("/api/transfers", payment("DFSP_A", "DFSP_B", "1000"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.feePercentage").value(1.50))
                .andExpect(jsonPath("$.totalAmount").value(1015.00))
                .andExpect(jsonPath("$.message").value("ACCEPTED (ref B-PAY-IT000001)"))
                .andReturn();
        UUID transactionId = transactionIdOf(result);

        // DFSP-B received its own format: paisa, clientRef, senderDfsp. DFSP-A was not called.
        assertThat(dfspB.requestCount()).isEqualTo(1);
        assertThat(dfspB.lastRequestBody())
                .contains("\"clientRef\":\"" + transactionId + "\"")
                .contains("\"senderDfsp\":\"DFSP_A\"")
                .contains("\"amountInPaisa\":100000")
                .contains("\"feeInPaisa\":1500");
        assertThat(dfspA.requestCount()).isZero();

        Transaction saved = reload(transactionId);
        assertThat(saved.getSourceProvider().getCode()).isEqualTo("DFSP_A");
        assertThat(saved.getDestinationProvider().getCode()).isEqualTo("DFSP_B");
        assertThat(saved.getAmount()).isEqualByComparingTo("1000.00");
        assertThat(saved.getFeePercentage()).isEqualByComparingTo("1.50");
        assertThat(saved.getFeeAmount()).isEqualByComparingTo("15.00");
        assertThat(saved.getTotalAmount()).isEqualByComparingTo("1015.00");
        assertThat(saved.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void transferBToAIsSentToDfspAInItsFormatAndPersisted() throws Exception {
        MvcResult result = postJson("/api/transfers", payment("DFSP_B", "DFSP_A", "1000"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.feePercentage").value(1.00))
                .andExpect(jsonPath("$.feeAmount").value(10.00))
                .andExpect(jsonPath("$.totalAmount").value(1010.00))
                .andReturn();
        UUID transactionId = transactionIdOf(result);

        assertThat(dfspA.requestCount()).isEqualTo(1);
        assertThat(dfspA.lastRequestBody())
                .contains("\"transactionId\":\"" + transactionId + "\"")
                .contains("\"sourceProvider\":\"DFSP_B\"")
                .contains("\"amount\":1000.00")
                .contains("\"fee\":10.00");
        assertThat(dfspB.requestCount()).isZero();

        Transaction saved = reload(transactionId);
        assertThat(saved.getDestinationProvider().getCode()).isEqualTo("DFSP_A");
        assertThat(saved.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
    }

    // ---- Same-provider transfers: source and destination are allowed to be equal ----

    @Test
    void transferAToAUsesDfspAsOwnStrategyAndIsPersisted() throws Exception {
        MvcResult result = postJson("/api/transfers", payment("DFSP_A", "DFSP_A", "1000"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.feePercentage").value(1.00))
                .andExpect(jsonPath("$.feeAmount").value(10.00))
                .andExpect(jsonPath("$.totalAmount").value(1010.00))
                .andReturn();
        UUID transactionId = transactionIdOf(result);

        // Destination is DFSP_A, so DFSP-A's own adapter and fee are used normally,
        // even though the source is also DFSP_A. DFSP-B is never called.
        assertThat(dfspA.requestCount()).isEqualTo(1);
        assertThat(dfspA.lastRequestBody())
                .contains("\"transactionId\":\"" + transactionId + "\"")
                .contains("\"sourceProvider\":\"DFSP_A\"")
                .contains("\"amount\":1000.00")
                .contains("\"fee\":10.00");
        assertThat(dfspB.requestCount()).isZero();

        Transaction saved = reload(transactionId);
        assertThat(saved.getSourceProvider().getCode()).isEqualTo("DFSP_A");
        assertThat(saved.getDestinationProvider().getCode()).isEqualTo("DFSP_A");
        assertThat(saved.getSourceProvider().getId()).isEqualTo(saved.getDestinationProvider().getId());
        assertThat(saved.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
    }

    @Test
    void transferBToBUsesDfspBsOwnStrategyAndIsPersisted() throws Exception {
        MvcResult result = postJson("/api/transfers", payment("DFSP_B", "DFSP_B", "1000"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.feePercentage").value(1.50))
                .andExpect(jsonPath("$.feeAmount").value(15.00))
                .andExpect(jsonPath("$.totalAmount").value(1015.00))
                .andReturn();
        UUID transactionId = transactionIdOf(result);

        assertThat(dfspB.requestCount()).isEqualTo(1);
        assertThat(dfspB.lastRequestBody())
                .contains("\"clientRef\":\"" + transactionId + "\"")
                .contains("\"senderDfsp\":\"DFSP_B\"")
                .contains("\"amountInPaisa\":100000")
                .contains("\"feeInPaisa\":1500");
        assertThat(dfspA.requestCount()).isZero();

        Transaction saved = reload(transactionId);
        assertThat(saved.getSourceProvider().getCode()).isEqualTo("DFSP_B");
        assertThat(saved.getDestinationProvider().getCode()).isEqualTo("DFSP_B");
        assertThat(saved.getSourceProvider().getId()).isEqualTo(saved.getDestinationProvider().getId());
        assertThat(saved.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
    }

    @Test
    void savedTransactionKeepsItsFeeWhenProviderFeeChangesLater() throws Exception {
        UUID transactionId = transactionIdOf(postJson("/api/transfers", payment("DFSP_A", "DFSP_B", "1000"))
                .andExpect(status().isCreated())
                .andReturn());

        configureProvider("DFSP_B", "2.00", dfspB.baseUrl());

        Transaction saved = reload(transactionId);
        assertThat(saved.getDestinationProvider().getFeePercentage()).isEqualByComparingTo("2.00");
        assertThat(saved.getFeePercentage()).isEqualByComparingTo("1.50");
        assertThat(saved.getFeeAmount()).isEqualByComparingTo("15.00");
    }

    // ---- 8. Failed DFSP response ----

    @Test
    void transferRejectedByDfspIsSavedAsFailed() throws Exception {
        dfspB.reset("""
                {"result":"REJECTED","paymentRef":"B-PAY-IT000002","reason":"Amount exceeds DFSP-B limit"}
                """);

        MvcResult result = postJson("/api/transfers", payment("DFSP_A", "DFSP_B", "30000"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.message").value("Amount exceeds DFSP-B limit (ref B-PAY-IT000002)"))
                .andReturn();

        assertThat(reload(transactionIdOf(result)).getStatus()).isEqualTo(TransactionStatus.FAILED);
    }

    @Test
    void transferToUnreachableDfspIsSavedAsFailed() throws Exception {
        configureProvider("DFSP_B", "1.50", FakeDfsp.unusedBaseUrl());

        MvcResult result = postJson("/api/transfers", payment("DFSP_A", "DFSP_B", "1000"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.message").value("Destination DFSP is unavailable"))
                .andReturn();

        assertThat(reload(transactionIdOf(result)).getStatus()).isEqualTo(TransactionStatus.FAILED);
    }

    // ---- 9. File logging ----

    @Test
    void transferIsWrittenToTheLogFile() throws Exception {
        UUID transactionId = transactionIdOf(postJson("/api/transfers", payment("DFSP_A", "DFSP_B", "1000"))
                .andExpect(status().isCreated())
                .andReturn());

        // logging.file.name: logs/payment-router.log, or target/test-logs/... when run by Maven.
        Path logFile = Path.of(environment.getRequiredProperty("logging.file.name"));
        assertThat(logFile).exists();
        assertThat(Files.readString(logFile, StandardCharsets.UTF_8))
                .contains("DFSP-B request: POST " + dfspB.baseUrl() + "/v1/payments/receive")
                .contains("Transfer " + transactionId + " succeeded");
    }

    // ---- helpers ----

    private void configureProvider(String code, String feePercentage, String baseUrl) {
        Provider provider = providerRepository.findByCode(code).orElseThrow();
        provider.setFeePercentage(new BigDecimal(feePercentage));
        provider.setBaseUrl(baseUrl);
        entityManager.flush();
    }

    private ResultActions postJson(String path, String body) throws Exception {
        return mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static String payment(String source, String destination, String amount) {
        return """
                {"sourceProviderCode":"%s","destinationProviderCode":"%s","amount":%s}
                """.formatted(source, destination, amount);
    }

    private static UUID transactionIdOf(MvcResult result) throws Exception {
        return UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.transactionId"));
    }

    /** Reads the row again from PostgreSQL instead of Hibernate's cache. */
    private Transaction reload(UUID transactionId) {
        entityManager.flush();
        entityManager.clear();
        return transactionRepository.findAll().stream()
                .filter(transaction -> transaction.getTransactionId().equals(transactionId))
                .findFirst()
                .orElseThrow();
    }

    /** Minimal HTTP server standing in for a DFSP: returns a fixed JSON body and records requests. */
    private static final class FakeDfsp {

        private final HttpServer server;
        private volatile String responseBody;
        private volatile String lastRequestBody;
        private volatile int requestCount;

        FakeDfsp(String path) throws IOException {
            server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext(path, exchange -> {
                lastRequestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                requestCount++;
                byte[] body = responseBody.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(body);
                }
            });
            server.start();
        }

        String baseUrl() {
            return "http://localhost:" + server.getAddress().getPort();
        }

        void reset(String response) {
            responseBody = response.strip();
            lastRequestBody = null;
            requestCount = 0;
        }

        String lastRequestBody() {
            return lastRequestBody;
        }

        int requestCount() {
            return requestCount;
        }

        void stop() {
            server.stop(0);
        }

        /** A local port with nothing listening on it, so connections are refused. */
        static String unusedBaseUrl() throws IOException {
            try (ServerSocket socket = new ServerSocket(0)) {
                return "http://localhost:" + socket.getLocalPort();
            }
        }
    }
}
