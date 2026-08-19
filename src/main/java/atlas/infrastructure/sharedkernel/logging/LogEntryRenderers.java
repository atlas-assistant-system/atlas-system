package atlas.infrastructure.sharedkernel.logging;

import atlas.application.sharedkernel.logging.LogEntryRenderer;
import atlas.application.sharedkernel.logging.PlainLogEntryRenderer;
import java.io.Console;
import java.time.ZoneId;

public final class LogEntryRenderers {

    public static final String FORMAT_PROPERTY = "log.format";
    public static final String FORMAT_VARIABLE = "LOG_FORMAT";
    public static final String CONSOLE_FORMAT = "console";
    public static final String PLAIN_FORMAT = "plain";

    private LogEntryRenderers() {}

    public static LogEntryRenderer forCurrentConsole() {
        return forConsole(colorIsSupported(), ZoneId.systemDefault());
    }

    public static LogEntryRenderer forConsole(boolean colorSupported, ZoneId zone) {
        if (!colorSupported) {
            return new PlainLogEntryRenderer();
        }

        return new ConsoleLogEntryRenderer(zone);
    }

    public static boolean colorIsSupported() {
        return resolve(requestedFormat(), System.getenv("NO_COLOR"), System.console());
    }

    static boolean resolve(String requestedFormat, String noColor, Console console) {
        if (CONSOLE_FORMAT.equalsIgnoreCase(requestedFormat)) {
            return true;
        }

        if (PLAIN_FORMAT.equalsIgnoreCase(requestedFormat)) {
            return false;
        }

        if (noColor != null && !noColor.isEmpty()) {
            return false;
        }

        return console != null && console.isTerminal();
    }

    private static String requestedFormat() {
        var property = System.getProperty(FORMAT_PROPERTY);

        return property != null ? property : System.getenv(FORMAT_VARIABLE);
    }
}
