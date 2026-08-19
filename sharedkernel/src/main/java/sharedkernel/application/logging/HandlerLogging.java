package sharedkernel.application.logging;

import java.lang.System.Logger.Level;
import java.time.Clock;
import java.time.Instant;
import sharedkernel.domain.results.Result;

public final class HandlerLogging {

    private HandlerLogging() {}

    public static HandlerLogEntry entryFor(
        Object message, LogKind kind, Object outcomeSource, Throwable thrown, long durationMs, Clock clock) {
        var outcome = outcomeOf(outcomeSource, thrown);

        return new HandlerLogEntry(
            Instant.now(clock),
            kind,
            message.getClass().getSimpleName(),
            outcome,
            durationMs,
            errorCodeOf(outcomeSource),
            exceptionOf(thrown),
            CorrelationContext.current().orElse(null),
            summaryOf(message));
    }

    public static void write(System.Logger log, LogEntryRenderer renderer, HandlerLogEntry entry, Throwable thrown,
        Level normalLevel) {
        var level = entry.outcome() == LogOutcome.ERROR ? Level.ERROR : normalLevel;
        var line = renderer.render(entry);

        if (thrown != null) {
            log.log(level, line, thrown);
            return;
        }

        log.log(level, line);
    }

    public static long elapsedMs(long startedAtNanos) {
        return (System.nanoTime() - startedAtNanos) / 1_000_000;
    }

    private static LogOutcome outcomeOf(Object result, Throwable thrown) {
        if (thrown != null) {
            return LogOutcome.ERROR;
        }

        if (result instanceof Result<?> outcome && outcome.isFailure()) {
            return LogOutcome.FAILURE;
        }

        return LogOutcome.SUCCESS;
    }

    private static String errorCodeOf(Object result) {
        if (result instanceof Result<?> outcome && outcome.isFailure()) {
            return outcome.error().code();
        }

        return null;
    }

    private static String exceptionOf(Throwable thrown) {
        if (thrown == null) {
            return null;
        }

        return thrown.getClass().getSimpleName();
    }

    private static String summaryOf(Object message) {
        if (message instanceof LoggableSummary loggable) {
            return HandlerLogEntry.sanitize(loggable.logSummary());
        }

        return null;
    }
}
