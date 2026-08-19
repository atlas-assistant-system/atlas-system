package atlas.app.presence;

import atlas.domain.presence.vos.MatchThreshold;
import atlas.domain.presence.vos.SessionDuration;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.function.UnaryOperator;

public record PresenceSettings(
    int port,
    Path dataDirectory,
    boolean maintenanceMode,
    MatchThreshold matchThreshold,
    SessionDuration sessionDuration) {

    public static final int DEFAULT_PORT = 8081;
    public static final String DEFAULT_DATA_DIRECTORY = "data";
    public static final double DEFAULT_MATCH_THRESHOLD = 0.8;
    public static final long DEFAULT_SESSION_MINUTES = 15;

    public static final String PORT_PROPERTY = "presence.port";
    public static final String PORT_VARIABLE = "PRESENCE_PORT";
    public static final String DATA_DIRECTORY_PROPERTY = "presence.data.dir";
    public static final String DATA_DIRECTORY_VARIABLE = "PRESENCE_DATA_DIR";
    public static final String MATCH_THRESHOLD_PROPERTY = "presence.match.threshold";
    public static final String MATCH_THRESHOLD_VARIABLE = "PRESENCE_MATCH_THRESHOLD";
    public static final String SESSION_MINUTES_PROPERTY = "presence.session.minutes";
    public static final String SESSION_MINUTES_VARIABLE = "PRESENCE_SESSION_MINUTES";

    private static final int MAX_PORT = 65535;

    public static PresenceSettings fromEnvironment(String[] args) {
        return from(args, System::getProperty, System::getenv);
    }

    static PresenceSettings from(String[] args, UnaryOperator<String> properties, UnaryOperator<String> environment) {
        var port = read(properties, environment, PORT_PROPERTY, PORT_VARIABLE);
        var directory = read(properties, environment, DATA_DIRECTORY_PROPERTY, DATA_DIRECTORY_VARIABLE);
        var threshold = read(properties, environment, MATCH_THRESHOLD_PROPERTY, MATCH_THRESHOLD_VARIABLE);
        var sessionMinutes = read(properties, environment, SESSION_MINUTES_PROPERTY, SESSION_MINUTES_VARIABLE);

        return new PresenceSettings(
            port == null ? DEFAULT_PORT : parsePort(port),
            Path.of(directory == null ? DEFAULT_DATA_DIRECTORY : directory),
            Arrays.asList(args).contains("--maintenance"),
            MatchThreshold.of(threshold == null ? DEFAULT_MATCH_THRESHOLD : parseThreshold(threshold)),
            SessionDuration.of(Duration.ofMinutes(
                sessionMinutes == null ? DEFAULT_SESSION_MINUTES : parseSessionMinutes(sessionMinutes))));
    }

    private static String read(
        UnaryOperator<String> properties, UnaryOperator<String> environment, String property, String variable) {
        var value = properties.apply(property);
        if (value == null) {
            value = environment.apply(variable);
        }
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static int parsePort(String value) {
        final int port;
        try {
            port = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid port '" + value + "': must be a whole number", e);
        }
        if (port < 0 || port > MAX_PORT) {
            throw new IllegalArgumentException("Invalid port '" + value + "': must be between 0 and " + MAX_PORT);
        }
        return port;
    }

    private static long parseLong(String value, String name) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid " + name + " '" + value + "': must be a whole number", e);
        }
    }

    private static double parseDouble(String value, String name) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid " + name + " '" + value + "': must be a number", e);
        }
    }

    private static double parseThreshold(String value) {
        var threshold = parseDouble(value, "match threshold");
        if (!Double.isFinite(threshold)
            || threshold < MatchThreshold.MIN_VALUE
            || threshold > MatchThreshold.MAX_VALUE) {
            throw new IllegalArgumentException("Invalid match threshold '" + value + "': must be between 0 and 1");
        }
        return threshold;
    }

    private static long parseSessionMinutes(String value) {
        var minutes = parseLong(value, "session minutes");
        var maximum = SessionDuration.MAX_DURATION.toMinutes();
        if (minutes < 1 || minutes > maximum) {
            throw new IllegalArgumentException(
                "Invalid session minutes '" + value + "': must be between 1 and " + maximum);
        }
        return minutes;
    }
}
