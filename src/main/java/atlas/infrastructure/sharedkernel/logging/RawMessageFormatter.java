package atlas.infrastructure.sharedkernel.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public final class RawMessageFormatter extends Formatter {

    @Override
    public String format(LogRecord record) {
        var line = new StringBuilder(formatMessage(record)).append(System.lineSeparator());

        if (record.getThrown() != null) {
            var trace = new StringWriter();
            record.getThrown().printStackTrace(new PrintWriter(trace));
            line.append(trace);
        }

        return line.toString();
    }
}
