/**
 * Strategy Pattern.
 * One strategy per destination DFSP isolates DFSP-specific behaviour. Services (QuoteService,
 * TransferService) select the matching strategy from the injected list of strategies by
 * comparing {@link com.paymentrouter.router.strategy.DfspStrategy#getProviderCode()}.
 */
package com.paymentrouter.router.strategy;
