package com.paymentrouter.router.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.paymentrouter.router.client.DfspAAdapter;
import com.paymentrouter.router.client.DfspBAdapter;

/**
 * Plain unit test: no Spring context, no database. Adapters are mocked because
 * strategy selection never calls them.
 */
class DfspStrategyFactoryTest {

    private final DfspAAdapter dfspAAdapter = mock(DfspAAdapter.class);
    private final DfspBAdapter dfspBAdapter = mock(DfspBAdapter.class);

    private final DfspStrategyFactory factory = new DfspStrategyFactory(
            List.of(new DfspAStrategy(dfspAAdapter), new DfspBStrategy(dfspBAdapter)));

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
        assertThatThrownBy(() -> new DfspStrategyFactory(
                List.of(new DfspAStrategy(dfspAAdapter), new DfspAStrategy(dfspAAdapter))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate key DFSP_A");
    }
}
