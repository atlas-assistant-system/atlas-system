package atlas.app.economy;

import java.nio.file.Path;
import java.util.function.UnaryOperator;

public record EconomySettings(int port, Path dataDirectory) {

    public static final int DEFAULT_PORT = 8080;
    public static final String DEFAULT_DATA_DIRECTORY = "data";

    public static final String PORT_PROPERTY = "economy.port";
    public static final String PORT_VARIABLE = "ECONOMIA_PORT";
    public static final String DATA_DIRECTORY_PROPERTY = "economy.data.dir";
    public static final String DATA_DIRECTORY_VARIABLE = "ECONOMIA_DATA_DIR";

    private static final int MAX_PORT = 65535;

    public static EconomySettings fromEnvironment() {
        return from(System::getProperty, System::getenv);
    }

    static EconomySettings from(UnaryOperator<String> properties, UnaryOperator<String> environment) {
        var port = read(properties, environment, PORT_PROPERTY, PORT_VARIABLE);
        var directory = read(properties, environment, DATA_DIRECTORY_PROPERTY, DATA_DIRECTORY_VARIABLE);

        return new EconomySettings(
            port == null ? DEFAULT_PORT : parsePort(port),
            Path.of(directory == null ? DEFAULT_DATA_DIRECTORY : directory));
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

    private static String rejected(String value) {
        return "Invalid port '" + value + "' from " + PORT_PROPERTY + " or " + PORT_VARIABLE + ".";
    }
}
