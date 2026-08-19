package atlas.infrastructure.sharedkernel.logging;

import static org.assertj.core.api.Assertions.assertThat;

import atlas.application.sharedkernel.logging.HandlerLogEntry;
import atlas.application.sharedkernel.logging.LogKind;
import atlas.application.sharedkernel.logging.LogOutcome;
import atlas.application.sharedkernel.logging.PlainLogEntryRenderer;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class ConsoleLogEntryRendererTest {

    private static final Instant AT = Instant.parse("2026-08-16T10:15:30Z");

    private final ConsoleLogEntryRenderer renderer = new ConsoleLogEntryRenderer(ZoneOffset.UTC);

    private static HandlerLogEntry entry(LogKind kind, LogOutcome outcome, String errorCode, String exception,
        String summary) {
        return new HandlerLogEntry(AT, kind, "ScheduleAppointment", outcome, 12, errorCode, exception, summary);
    }

    @Test
    void shouldRenderTheSpringStyleEnvelope() {
        var line = stripColors(renderer.render(entry(LogKind.COMMAND, LogOutcome.SUCCESS, null, null, "slot=09:00")));

        assertThat(line)
            .startsWith("2026-08-16T10:15:30.000Z  INFO " + ProcessHandle.current().pid() + " --- [atlas] [")
            .contains("] ScheduleAppointment : COMMAND SUCCESS duration=12ms summary=slot=09:00");
    }

    @Test
    void shouldRenderDebugForQueries() {
        var line = stripColors(renderer.render(entry(LogKind.QUERY, LogOutcome.SUCCESS, null, null, null)));

        assertThat(line).contains(" DEBUG ").contains("QUERY SUCCESS");
    }

    @Test
    void shouldRenderInfoAndTheCodeForBusinessFailures() {
        var line = stripColors(renderer.render(
            entry(LogKind.COMMAND, LogOutcome.FAILURE, "APPOINTMENT_CANCELLED", null, null)));

        assertThat(line).contains(" INFO ").contains("COMMAND FAILURE").contains("code=APPOINTMENT_CANCELLED");
    }

    @Test
    void shouldRenderErrorAndTheExceptionForUnexpectedFailures() {
        var line = stripColors(renderer.render(
            entry(LogKind.COMMAND, LogOutcome.ERROR, null, "SQLiteException", null)));

        assertThat(line).contains("ERROR ").contains("COMMAND ERROR").contains("exception=SQLiteException");
    }

    @Test
    void shouldNameAnonymousVirtualThreads() throws InterruptedException {
        var rendered = new AtomicReference<String>();
        var thread = Thread.ofVirtual().unstarted(() -> rendered.set(stripColors(
            renderer.render(entry(LogKind.COMMAND, LogOutcome.SUCCESS, null, null, null)))));

        thread.start();
        thread.join();

        assertThat(rendered.get()).contains("[virtual-" + thread.threadId() + "]");
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
