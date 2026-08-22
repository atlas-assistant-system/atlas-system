package atlas.application.sharedkernel.cqrs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SimpleCommandBusTest {

    private final SimpleCommandBus bus = new SimpleCommandBus();

    @Test
    void shouldInvokeHandlerWhenCommandTypeIsRegistered() {
        bus.register(GreetCommand.class, command -> "hello " + command.name());

        var response = bus.dispatch(new GreetCommand("world"));

        assertThat(response).isEqualTo("hello world");
    }

    @Test
    void shouldThrowWhenNoHandlerIsRegistered() {
        assertThatThrownBy(() -> bus.dispatch(new UnregisteredCommand()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("UnregisteredCommand");
    }

    @Test
    void shouldThrowWhenRegisteringDuplicateHandler() {
        bus.register(GreetCommand.class, command -> "first");

        assertThatThrownBy(() -> bus.register(GreetCommand.class, command -> "second"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("GreetCommand");
    }
}
