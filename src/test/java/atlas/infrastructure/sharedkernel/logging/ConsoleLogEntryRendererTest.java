package atlas.infrastructure.sharedkernel.logging;

import static org.assertj.core.api.Assertions.assertThat;

import atlas.application.sharedkernel.logging.HandlerLogEntry;
import atlas.application.sharedkernel.logging.LogKind;
import atlas.application.sharedkernel.logging.LogOutcome;
import atlas.application.sharedkernel.logging.PlainLogEntryRenderer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class ConsoleLogEntryRendererTest {

    private static final Instant AT = Instant.parse("2026-08-16T10:15:30Z");

    private final ConsoleLogEntryRenderer renderer =
        new ConsoleLogEntryRenderer(ZoneOffset.UTC, StandardCharsets.UTF_8);

    @Test
    void shouldUseTheBarWhenTheOutputEncodingSupportsIt() {
        assertThat(ConsoleLogEntryRenderer.gutterFor(StandardCharsets.UTF_8))
            .isEqualTo(ConsoleLogEntryRenderer.GUTTER);
    }

    @Test
    void shouldFallBackToAnAsciiGutterWhenTheOutputEncodingCannotRenderTheBar() {
        assertThat(ConsoleLogEntryRenderer.gutterFor(StandardCharsets.US_ASCII))
            .isEqualTo(ConsoleLogEntryRenderer.ASCII_GUTTER);
    }

    @Test
    void shouldRenderTheAsciiGutterWhenTheOutputEncodingCannotRenderTheBar() {
        var ascii = new ConsoleLogEntryRenderer(ZoneOffset.UTC, StandardCharsets.US_ASCII);

        var line = ascii.render(entry(LogKind.COMMAND, LogOutcome.SUCCESS, null, null, null));

        assertThat(line).startsWith(ConsoleLogEntryRenderer.GREEN + ConsoleLogEntryRenderer.ASCII_GUTTER);
    }

    private static HandlerLogEntry entry(LogKind kind, LogOutcome outcome, String errorCode, String exception,
        String summary) {
        return new HandlerLogEntry(AT, kind, "ScheduleAppointment", outcome, 12, errorCode, exception, null, summary);
    }

    @Test
    void shouldStartWithGreenGutterWhenCommandSucceeds() {
        var line = renderer.render(entry(LogKind.COMMAND, LogOutcome.SUCCESS, null, null, null));

        assertThat(line).startsWith(ConsoleLogEntryRenderer.GREEN + ConsoleLogEntryRenderer.GUTTER);
    }

    @Test
    void shouldStartWithDimGutterWhenQuerySucceeds() {
        var line = renderer.render(entry(LogKind.QUERY, LogOutcome.SUCCESS, null, null, null));

        assertThat(line).startsWith(ConsoleLogEntryRenderer.DIM + ConsoleLogEntryRenderer.GUTTER);
    }

    @Test
    void shouldStartWithAmberGutterWhenBusinessFailure() {
        var line = renderer.render(
            entry(LogKind.COMMAND, LogOutcome.FAILURE, "APPOINTMENT_CANCELLED", null, null));

        assertThat(line).startsWith(ConsoleLogEntryRenderer.AMBER + ConsoleLogEntryRenderer.GUTTER);
        assertThat(line).contains("APPOINTMENT_CANCELLED");
    }

    @Test
    void shouldStartWithRedGutterWhenException() {
        var line = renderer.render(entry(LogKind.COMMAND, LogOutcome.ERROR, null, "SQLiteException", null));

        assertThat(line).startsWith(ConsoleLogEntryRenderer.RED + ConsoleLogEntryRenderer.GUTTER);
        assertThat(line).contains("SQLiteException");
    }

    @Test
    void shouldRenderTimeNameAndDurationWhenStripped() {
        var line = stripColors(renderer.render(entry(LogKind.COMMAND, LogOutcome.SUCCESS, null, null, "slot=09:00")));

        assertThat(line).isEqualTo("▌ 10:15:30  ScheduleAppointment       12ms   slot=09:00");
    }

    @Test
    void shouldAlignNamesOfDifferentLengthWhenRendered() {
        var shortName = new HandlerLogEntry(AT, LogKind.COMMAND, "Cancel", LogOutcome.SUCCESS, 1, null, null, null,
            null);
        var longName = new HandlerLogEntry(AT, LogKind.COMMAND, "ScheduleAppointment", LogOutcome.SUCCESS, 1, null,
            null, null, null);

        assertThat(stripColors(renderer.render(shortName)).indexOf("1ms"))
            .isEqualTo(stripColors(renderer.render(longName)).indexOf("1ms"));
    }

    @Test
    void shouldFallBackToPlainRendererWhenColorIsNotSupported() {
        var plain = LogEntryRenderers.forConsole(false, ZoneOffset.UTC);

        assertThat(plain).isInstanceOf(PlainLogEntryRenderer.class);
        assertThat(plain.render(entry(LogKind.COMMAND, LogOutcome.FAILURE, "APPOINTMENT_CANCELLED", null, null)))
            .contains("outcome=failure")
            .doesNotContain(ConsoleLogEntryRenderer.ESC);
    }

    private static String stripColors(String line) {
        return line.replaceAll(ConsoleLogEntryRenderer.ESC + "\\[[0-9]+m", "");
    }
}
