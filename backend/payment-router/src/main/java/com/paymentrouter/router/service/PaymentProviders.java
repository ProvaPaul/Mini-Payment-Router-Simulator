package com.paymentrouter.router.service;

import com.paymentrouter.router.entity.Provider;

/**
 * The two providers of a validated quote or transfer request.
 *
 * @param source      provider the money is sent from
 * @param destination provider the money is sent to
 */
public record PaymentProviders(Provider source, Provider destination) {
}
