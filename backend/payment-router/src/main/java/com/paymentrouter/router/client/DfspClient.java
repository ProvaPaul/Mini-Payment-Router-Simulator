package com.paymentrouter.router.client;

/**
 * Adapter Pattern: the router's single, provider-neutral way to talk to a DFSP.
 * <p>
 * Each implementation translates the common {@link DfspTransferRequest} into one DFSP's own
 * HTTP API format and translates that DFSP's response back into a {@link DfspTransferResult}.
 * Code outside the adapters never sees DFSP-specific URLs, field names, money units or status values.
 */
public interface DfspClient {

    /**
     * Sends a transfer to the DFSP.
     *
     * @param baseUrl DFSP base URL from the providers table, e.g. http://dfsp-b:8082
     * @param request provider-neutral transfer data
     * @return the DFSP's decision translated into the router's common result
     * @throws com.paymentrouter.router.exception.DfspCommunicationException if the DFSP cannot be reached
     *         or returns a response the adapter cannot understand
     */
    DfspTransferResult transfer(String baseUrl, DfspTransferRequest request);
}
