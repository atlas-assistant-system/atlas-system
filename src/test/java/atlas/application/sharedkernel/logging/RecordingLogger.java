package atlas.application.sharedkernel.logging;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

final class RecordingLogger implements System.Logger {

    private final List<RecordedLogEntry> entries = new ArrayList<>();

    List<RecordedLogEntry> entries() {
        return entries;
    }

    RecordedLogEntry single() {
        if (entries.size() != 1) {
            throw new IllegalStateException("Expected exactly one log entry but got " + entries.size());
        }

        return entries.getFirst();
    }

    @Override
    public String getName() {
        return "recording";
    }

    @Override
    public boolean isLoggable(Level level) {
        return true;
    }

    @Override
    public void log(Level level, ResourceBundle bundle, String message, Object... params) {
        entries.add(new RecordedLogEntry(level, message, null));
    }

    @Override
    public void log(Level level, ResourceBundle bundle, String message, Throwable thrown) {
        entries.add(new RecordedLogEntry(level, message, thrown));
    }
}
