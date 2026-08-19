package sharedkernel.infrastructure.logging;

import java.nio.charset.Charset;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import sharedkernel.application.logging.HandlerLogEntry;
import sharedkernel.application.logging.LogEntryRenderer;
import sharedkernel.application.logging.LogKind;
import sharedkernel.application.logging.LogOutcome;

public final class ConsoleLogEntryRenderer implements LogEntryRenderer {

    public static final String ESC = String.valueOf((char) 27);
    public static final String GUTTER = "▌";
    public static final String ASCII_GUTTER = "|";
    public static final String RESET = ESC + "[0m";
    public static final String DIM = ESC + "[90m";
    public static final String GREEN = ESC + "[32m";
    public static final String AMBER = ESC + "[33m";
    public static final String RED = ESC + "[31m";

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final int NAME_WIDTH = 24;

    private final ZoneId zone;
    private final String gutter;

    public ConsoleLogEntryRenderer(ZoneId zone) {
        this(zone, outputCharset());
    }

    public ConsoleLogEntryRenderer(ZoneId zone, Charset outputCharset) {
        this.zone = zone;
        this.gutter = gutterFor(outputCharset);
    }

    static String gutterFor(Charset outputCharset) {
        if (outputCharset.newEncoder().canEncode(GUTTER)) {
            return GUTTER;
        }

        return ASCII_GUTTER;
    }

    private static Charset outputCharset() {
        var configured = System.getProperty("stdout.encoding");

        if (configured == null) {
            return Charset.defaultCharset();
        }

        return Charset.forName(configured, Charset.defaultCharset());
    }

    @Override
    public String render(HandlerLogEntry entry) {
        var time = LocalTime.ofInstant(entry.occurredAt(), zone).format(TIME);
        var name = pad(entry.name());
        var duration = String.format("%6s", entry.durationMs() + "ms");
        var line = new StringBuilder();

        line.append(gutterColorFor(entry)).append(gutter).append(RESET).append(' ');
        line.append(DIM).append(time).append(RESET).append("  ");
        line.append(entry.kind() == LogKind.QUERY ? DIM + name + RESET : name);
        line.append(DIM).append(duration).append(RESET);

        var detail = entry.detail();
        if (!detail.isEmpty()) {
            line.append("   ").append(detailColorFor(entry)).append(detail).append(RESET);
        }

        return line.toString();
    }

    private static String gutterColorFor(HandlerLogEntry entry) {
        return switch (entry.outcome()) {
            case ERROR -> RED;
            case FAILURE -> AMBER;
            case SUCCESS -> entry.kind() == LogKind.QUERY ? DIM : GREEN;
        };
    }

    private static String detailColorFor(HandlerLogEntry entry) {
        if (entry.outcome() == LogOutcome.ERROR) {
            return RED;
        }

        if (entry.outcome() == LogOutcome.FAILURE) {
            return AMBER;
        }

        return DIM;
    }

    private static String pad(String name) {
        if (name.length() >= NAME_WIDTH) {
            return name + " ";
        }

        return name + " ".repeat(NAME_WIDTH - name.length());
    }
}
