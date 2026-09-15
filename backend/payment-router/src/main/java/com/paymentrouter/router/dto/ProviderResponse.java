package com.paymentrouter.router.dto;

/**
 * A provider option for the UI dropdowns. Internal configuration (base URL, fee) is not exposed.
 *
 * @param code stable identifier sent back in quote and transfer requests, e.g. DFSP_A
 * @param name display name, e.g. DFSP-A
 */
public record ProviderResponse(String code, String name) {
}
