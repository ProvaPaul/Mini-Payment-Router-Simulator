package com.paymentrouter.router.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.paymentrouter.router.entity.Transaction;

/**
 * Data access for {@link Transaction}.
 * <p>
 * Saving a transaction uses the inherited {@code save(Transaction)} method.
 * No custom queries are needed yet.
 */
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
}
