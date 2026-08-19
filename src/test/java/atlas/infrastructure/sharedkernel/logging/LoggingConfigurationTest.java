package atlas.infrastructure.sharedkernel.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;

class LoggingConfigurationTest {

    @Test
    void shouldLoadTheRawMessageFormatter() throws IOException {
        try (var resource = getClass().getResourceAsStream("/logging.properties")) {
            LogManager.getLogManager().readConfiguration(resource);
        }

        assertThat(Logger.getLogger("").getHandlers())
            .singleElement()
            .extracting(handler -> handler.getFormatter())
            .isInstanceOf(RawMessageFormatter.class);
    }
}
