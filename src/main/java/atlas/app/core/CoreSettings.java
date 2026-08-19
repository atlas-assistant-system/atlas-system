package atlas.app.core;

import java.util.function.UnaryOperator;

public record CoreSettings(double latitude, double longitude) {

    public static final double DEFAULT_LATITUDE = 40.4168;
    public static final double DEFAULT_LONGITUDE = -3.7038;

    public static final String LATITUDE_PROPERTY = "atlas.weather.latitude";
    public static final String LATITUDE_VARIABLE = "ATLAS_WEATHER_LATITUDE";
    public static final String LONGITUDE_PROPERTY = "atlas.weather.longitude";
    public static final String LONGITUDE_VARIABLE = "ATLAS_WEATHER_LONGITUDE";

    private static final double MAX_LATITUDE = 90;
    private static final double MAX_LONGITUDE = 180;

    public static CoreSettings fromEnvironment() {
        return from(System::getProperty, System::getenv);
    }

    static CoreSettings from(UnaryOperator<String> properties, UnaryOperator<String> environment) {
        return new CoreSettings(
            coordinate(properties, environment, LATITUDE_PROPERTY, LATITUDE_VARIABLE, DEFAULT_LATITUDE, MAX_LATITUDE),
            coordinate(
                properties, environment, LONGITUDE_PROPERTY, LONGITUDE_VARIABLE, DEFAULT_LONGITUDE, MAX_LONGITUDE));
    }

    private static double coordinate(
        UnaryOperator<String> properties,
        UnaryOperator<String> environment,
        String property,
        String variable,
        double fallback,
        double limit) {
        var raw = properties.apply(property);
        var source = property;
        if (raw == null || raw.isBlank()) {
            raw = environment.apply(variable);
            source = variable;
        }
        if (raw == null || raw.isBlank()) {
            return fallback;
        }

        double value;
        try {
            value = Double.parseDouble(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                "Invalid coordinate '" + raw.trim() + "' from " + source + ": it must be a number.", e);
        }
        if (value < -limit || value > limit) {
            throw new IllegalArgumentException(
                "Invalid coordinate '" + raw.trim() + "' from " + source
                    + ": it must be between " + -limit + " and " + limit + ".");
        }

        return value;
    }
}
