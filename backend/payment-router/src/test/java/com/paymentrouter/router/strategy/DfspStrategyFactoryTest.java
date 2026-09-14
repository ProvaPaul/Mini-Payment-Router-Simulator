package com.paymentrouter.router.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Plain unit test: no Spring context, no database.
 */
class DfspStrategyFactoryTest {

    private final DfspStrategyFactory factory =
            new DfspStrategyFactory(List.of(new DfspAStrategy(), new DfspBStrategy()));

    @Test
    void returnsStrategyMatchingTheProviderCode() {
        assertThat(factory.getStrategy("DFSP_A")).isInstanceOf(DfspAStrategy.class);
        assertThat(factory.getStrategy("DFSP_B")).isInstanceOf(DfspBStrategy.class);
    }

    @Test
    void failsWhenNoStrategyIsRegisteredForTheCode() {
        assertThatThrownBy(() -> factory.getStrategy("DFSP_C"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No DFSP strategy registered for provider DFSP_C");
    }

    @Test
    void failsAtCreationWhenTwoStrategiesUseTheSameCode() {
        DfspStrategy duplicateA = () -> "DFSP_A";

        assertThatThrownBy(() -> new DfspStrategyFactory(List.of(new DfspAStrategy(), duplicateA)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate key DFSP_A");
    }
}
