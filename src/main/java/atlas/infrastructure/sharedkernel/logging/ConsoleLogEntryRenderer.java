package atlas.infrastructure.sharedkernel.logging;

import atlas.application.sharedkernel.logging.HandlerLogEntry;
import atlas.application.sharedkernel.logging.LogEntryRenderer;
import atlas.application.sharedkernel.logging.LogKind;
import atlas.application.sharedkernel.logging.LogOutcome;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class ConsoleLogEntryRenderer implements LogEntryRenderer {

    public static final String ESC = String.valueOf((char) 27);
    public static final String RESET = ESC + "[0m";
    public static final String DIM = ESC + "[90m";
    public static final String GREEN = ESC + "[32m";
    public static final String AMBER = ESC + "[33m";
    public static final String RED = ESC + "[31m";

    private static final String APPLICATION_NAME = "atlas";
    private static final long PROCESS_ID = ProcessHandle.current().pid();
    private static final DateTimeFormatter TIMESTAMP =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    private final ZoneId zone;

    public ConsoleLogEntryRenderer(ZoneId zone) {
        this.zone = zone;
    }

    @Override
    public String render(HandlerLogEntry entry) {
        var timestamp = entry.occurredAt().atZone(zone).format(TIMESTAMP);
        var line = new StringBuilder();

        line.append(DIM).append(timestamp).append(RESET).append(' ');
        line.append(levelColorFor(entry))
            .append(String.format("%5s", levelFor(entry)))
            .append(RESET)
            .append(' ');
        line.append(DIM)
            .append(PROCESS_ID)
            .append(" --- [")
            .append(APPLICATION_NAME)
            .append("] [")
            .append(currentThreadName())
            .append("] ")
            .append(RESET);
        line.append(entry.name()).append(" : ");
        line.append(outcomeColorFor(entry))
            .append(entry.kind().name())
            .append(' ')
            .append(entry.outcome().name())
            .append(RESET);
        line.append(DIM).append(" duration=").append(entry.durationMs()).append("ms").append(RESET);

        var detail = entry.detail();
        if (!detail.isEmpty()) {
            line.append(' ').append(outcomeColorFor(entry)).append(detail).append(RESET);
        }

        return line.toString();
    }

    static String currentThreadName() {
        var thread = Thread.currentThread();
        if (!thread.getName().isBlank()) {
            return thread.getName();
        }

        return (thread.isVirtual() ? "virtual-" : "thread-") + thread.threadId();
    }

    private static String levelFor(HandlerLogEntry entry) {
        if (entry.outcome() == LogOutcome.ERROR) {
            return "ERROR";
        }

        return entry.kind() == LogKind.QUERY ? "DEBUG" : "INFO";
    }

    private static String levelColorFor(HandlerLogEntry entry) {
        if (entry.outcome() == LogOutcome.ERROR) {
            return RED;
        }

        return entry.kind() == LogKind.QUERY ? DIM : GREEN;
    }

    private static String outcomeColorFor(HandlerLogEntry entry) {
        return switch (entry.outcome()) {
            case ERROR -> RED;
            case FAILURE -> AMBER;
            case SUCCESS -> entry.kind() == LogKind.QUERY ? DIM : GREEN;
        };
    }
}
