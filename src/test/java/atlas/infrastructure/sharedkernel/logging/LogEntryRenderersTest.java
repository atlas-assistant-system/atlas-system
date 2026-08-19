package atlas.infrastructure.sharedkernel.logging;

import static org.assertj.core.api.Assertions.assertThat;

import atlas.application.sharedkernel.logging.PlainLogEntryRenderer;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class LogEntryRenderersTest {

    @Test
    void shouldUseTheConsoleRendererWhenTheFormatIsRequestedExplicitly() {
        assertThat(LogEntryRenderers.resolve(LogEntryRenderers.CONSOLE_FORMAT, null, null)).isTrue();
    }

    @Test
    void shouldUseThePlainRendererWhenTheFormatIsRequestedExplicitly() {
        assertThat(LogEntryRenderers.resolve(LogEntryRenderers.PLAIN_FORMAT, null, null)).isFalse();
    }

    @Test
    void shouldIgnoreNoColorWhenTheConsoleFormatWasRequestedExplicitly() {
        assertThat(LogEntryRenderers.resolve(LogEntryRenderers.CONSOLE_FORMAT, "1", null)).isTrue();
    }

    @Test
    void shouldHonourNoColorWhenNoFormatWasRequested() {
        assertThat(LogEntryRenderers.resolve(null, "1", null)).isFalse();
    }

    @Test
    void shouldFallBackToPlainWhenThereIsNoConsole() {
        assertThat(LogEntryRenderers.resolve(null, null, null)).isFalse();
    }

    @Test
    void shouldAcceptTheRequestedFormatRegardlessOfCase() {
        assertThat(LogEntryRenderers.resolve("CONSOLE", null, null)).isTrue();
        assertThat(LogEntryRenderers.resolve("Plain", null, null)).isFalse();
    }

    @Test
    void shouldBuildThePlainRendererWhenColorIsNotSupported() {
        assertThat(LogEntryRenderers.forConsole(false, ZoneId.of("UTC"))).isInstanceOf(PlainLogEntryRenderer.class);
    }

    @Test
    void shouldBuildTheConsoleRendererWhenColorIsSupported() {
        assertThat(LogEntryRenderers.forConsole(true, ZoneId.of("UTC"))).isInstanceOf(ConsoleLogEntryRenderer.class);
    }
}
