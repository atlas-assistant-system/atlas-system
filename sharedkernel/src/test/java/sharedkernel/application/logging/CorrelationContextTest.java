package sharedkernel.application.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class CorrelationContextTest {

    @Test
    void shouldBeEmptyWhenNoScopeIsBound() {
        assertThat(CorrelationContext.current()).isEmpty();
    }

    @Test
    void shouldExposeIdWhenScopeIsBound() {
        var seen = new AtomicReference<String>();

        CorrelationContext.runWith("a3f9c1", () -> seen.set(CorrelationContext.current().orElse(null)));

        assertThat(seen.get()).isEqualTo("a3f9c1");
    }

    @Test
    void shouldUnbindWhenScopeEnds() {
        CorrelationContext.runWith("a3f9c1", () -> {});

        assertThat(CorrelationContext.current()).isEmpty();
    }
}
