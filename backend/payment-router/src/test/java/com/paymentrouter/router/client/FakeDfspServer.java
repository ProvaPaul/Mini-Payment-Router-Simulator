package com.paymentrouter.router.client;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;

import com.sun.net.httpserver.HttpServer;

/**
 * A tiny in-process HTTP server standing in for a DFSP, used instead of MockRestServiceServer.
 * <p>
 * The adapter under test makes a real HTTP call over a real socket to this server, so the
 * request it actually sends and the response it actually parses are both exercised for real —
 * without any Spring test infrastructure. This is the same technique the removed
 * {@code PaymentApiIntegrationTest} used for its fake DFSPs.
 */
final class FakeDfspServer {

    private final HttpServer server;
    private volatile int responseStatus = 200;
    private volatile String responseBody = "";
    private volatile long responseDelayMillis;
    private volatile String lastRequestBody;
    private volatile int requestCount;

    FakeDfspServer(String path) throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext(path, exchange -> {
            lastRequestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            requestCount++;
            if (responseDelayMillis > 0) {
                sleep(responseDelayMillis);
            }
            byte[] body = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(responseStatus, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();
    }

    String baseUrl() {
        return "http://localhost:" + server.getAddress().getPort();
    }

    /** Sets the response the next request(s) will receive. */
    void respondWith(int status, String jsonBody) {
        this.responseStatus = status;
        this.responseBody = jsonBody.strip();
        this.responseDelayMillis = 0;
    }

    /** Like {@link #respondWith}, but waits {@code delayMillis} before answering, to trigger a real client-side read timeout. */
    void respondAfterDelay(int status, String jsonBody, long delayMillis) {
        this.responseStatus = status;
        this.responseBody = jsonBody.strip();
        this.responseDelayMillis = delayMillis;
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

    /** A local port with nothing listening on it, so connecting to it is refused. */
    static String unusedBaseUrl() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return "http://localhost:" + socket.getLocalPort();
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
