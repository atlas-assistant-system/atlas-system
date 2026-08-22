package atlas.application.sharedkernel.cqrs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class SimpleQueryBusTest {

    private final SimpleQueryBus bus = new SimpleQueryBus();

    @Test
    void shouldInvokeHandlerWhenQueryTypeIsRegistered() {
        bus.register(ListNamesQuery.class, query -> List.of(query.prefix() + "-a", query.prefix() + "-b"));

        var response = bus.dispatch(new ListNamesQuery("x"));

        assertThat(response).containsExactly("x-a", "x-b");
    }

    @Test
    void shouldThrowWhenNoHandlerIsRegistered() {
        assertThatThrownBy(() -> bus.dispatch(new UnregisteredQuery()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("UnregisteredQuery");
    }

    @Test
    void shouldThrowWhenRegisteringDuplicateHandler() {
        bus.register(ListNamesQuery.class, query -> List.of());

        assertThatThrownBy(() -> bus.register(ListNamesQuery.class, query -> List.of()))
            .isInstanceOf(IllegalStateException.class);
    }
}
