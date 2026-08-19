package atlas.app.appointments;

import java.net.URI;
import java.nio.file.Path;
import java.util.function.UnaryOperator;

public record AppointmentsSettings(int port, Path dataDirectory, URI presenceUrl) {

    public static final int DEFAULT_PORT = 8080;
    public static final String DEFAULT_DATA_DIRECTORY = "data";
    public static final String DEFAULT_PRESENCE_URL = "http://localhost:8081";

    public static final String PORT_PROPERTY = "agenda.port";
    public static final String PORT_VARIABLE = "AGENDA_PORT";
    public static final String DATA_DIRECTORY_PROPERTY = "agenda.data.dir";
    public static final String DATA_DIRECTORY_VARIABLE = "AGENDA_DATA_DIR";
    public static final String PRESENCE_URL_PROPERTY = "agenda.presence.url";
    public static final String PRESENCE_URL_VARIABLE = "AGENDA_PRESENCE_URL";

    private static final int MAX_PORT = 65535;

    public static AppointmentsSettings fromEnvironment() {
        return from(System::getProperty, System::getenv);
    }

    static AppointmentsSettings from(UnaryOperator<String> properties, UnaryOperator<String> environment) {
        var port = read(properties, environment, PORT_PROPERTY, PORT_VARIABLE);
        var directory = read(properties, environment, DATA_DIRECTORY_PROPERTY, DATA_DIRECTORY_VARIABLE);
        var presenceUrl = read(properties, environment, PRESENCE_URL_PROPERTY, PRESENCE_URL_VARIABLE);

        return new AppointmentsSettings(
            port == null ? DEFAULT_PORT : parsePort(port),
            Path.of(directory == null ? DEFAULT_DATA_DIRECTORY : directory),
            parsePresenceUrl(presenceUrl == null ? DEFAULT_PRESENCE_URL : presenceUrl));
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
        int port;
        try {
            port = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(rejected(value) + " It must be a whole number.", e);
        }

        if (port < 0 || port > MAX_PORT) {
            throw new IllegalArgumentException(rejected(value) + " It must be between 0 and " + MAX_PORT + ".");
        }

        return port;
    }

    private static URI parsePresenceUrl(String value) {
        final URI url;
        try {
            url = URI.create(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid Presence URL '" + value + "'.", e);
        }

        if ((!("http".equals(url.getScheme()) || "https".equals(url.getScheme()))) || url.getHost() == null) {
            throw new IllegalArgumentException("Invalid Presence URL '" + value + "': it must be an HTTP URL.");
        }

        return url;
    }

    private static String rejected(String value) {
        return "Invalid port '" + value + "' from " + PORT_PROPERTY + " or " + PORT_VARIABLE + ".";
    }
}
