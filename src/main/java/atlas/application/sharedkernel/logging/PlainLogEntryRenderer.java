package atlas.application.sharedkernel.logging;

public final class PlainLogEntryRenderer implements LogEntryRenderer {

    @Override
    public String render(HandlerLogEntry entry) {
        var line = new StringBuilder();
        line.append("type=").append(entry.kind().label());
        line.append(" name=").append(entry.name());
        line.append(" outcome=").append(entry.outcome().label());
        line.append(" durationMs=").append(entry.durationMs());

        if (entry.errorCode() != null) {
            line.append(" errorCode=").append(entry.errorCode());
        }

        if (entry.exception() != null) {
            line.append(" exception=").append(entry.exception());
        }

        if (entry.summary() != null) {
            line.append(" summary=").append(entry.summary());
        }

        return line.toString();
    }
}
