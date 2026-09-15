package com.paymentrouter.router.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.paymentrouter.router.entity.Provider;
import com.paymentrouter.router.entity.ProviderStatus;

/**
 * Data access for {@link Provider}.
 * <p>
 * Inherited from {@link JpaRepository}: {@code findById(Long)}, {@code save}, {@code findAll}, ...
 * Spring Data creates the implementation at startup; no SQL is written here.
 */
public interface ProviderRepository extends JpaRepository<Provider, Long> {

    /**
     * Finds a provider by its unique business code, e.g. {@code DFSP_A}.
     * <p>
     * The query is derived from the method name:
     * {@code select p from Provider p where p.code = :code}
     *
     * @return the provider, or an empty Optional when no provider has this code
     */
    Optional<Provider> findByCode(String code);

    /**
     * Providers with the given status, ordered by code.
     * <p>
     * Derived query: {@code select p from Provider p where p.status = :status order by p.code asc}
     */
    List<Provider> findByStatusOrderByCodeAsc(ProviderStatus status);
}
