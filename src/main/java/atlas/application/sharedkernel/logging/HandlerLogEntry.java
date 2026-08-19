package atlas.application.sharedkernel.logging;

import java.time.Instant;

public record HandlerLogEntry(
    Instant occurredAt,
    LogKind kind,
    String name,
    LogOutcome outcome,
    long durationMs,
    String errorCode,
    String exception,
    String summary) {

    public static final int MAX_SUMMARY_LENGTH = 120;

    public String detail() {
        if (exception != null) {
            return "exception=" + exception;
        }

        if (errorCode != null) {
            return "code=" + errorCode;
        }

        return summary != null ? "summary=" + summary : "";
    }

    public static String sanitize(String text) {
        if (text == null) {
            return null;
        }

        var cleaned = new StringBuilder(text.length());
        for (int i = 0; i < text.length() && cleaned.length() < MAX_SUMMARY_LENGTH; i++) {
            char c = text.charAt(i);
            cleaned.append(Character.isISOControl(c) ? ' ' : c);
        }

        return cleaned.toString().trim();
    }
}
