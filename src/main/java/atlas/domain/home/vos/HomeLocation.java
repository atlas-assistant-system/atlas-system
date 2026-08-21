package atlas.domain.home.vos;

import atlas.domain.home.HomeErrors;
import atlas.domain.sharedkernel.ddd.ValueObject;
import atlas.domain.sharedkernel.exceptions.GuardException;
import atlas.domain.sharedkernel.guards.ObjectGuard;
import atlas.domain.sharedkernel.guards.StringGuard;
import atlas.domain.sharedkernel.results.Result;
import java.time.DateTimeException;
import java.time.ZoneId;

public record HomeLocation(String name, double latitude, double longitude, ZoneId timeZone) implements ValueObject {

    private static final int MAX_NAME_LENGTH = 120;

    public HomeLocation {
        name = StringGuard.notLongerThan(name, MAX_NAME_LENGTH, "name").trim();
        ObjectGuard.notNull(timeZone, "timeZone");
        if (!validCoordinates(latitude, longitude)) {
            throw GuardException.forParameter("coordinates", "must be valid latitude and longitude values");
        }
    }

    public static Result<HomeLocation> create(String name, double latitude, double longitude, String timeZone) {
        if (name == null || name.isBlank()) {
            return Result.failure(HomeErrors.LOCATION_NAME_REQUIRED);
        }
        var cleanName = name.trim();
        if (cleanName.length() > MAX_NAME_LENGTH) {
            return Result.failure(HomeErrors.LOCATION_NAME_TOO_LONG);
        }
        if (!validCoordinates(latitude, longitude)) {
            return Result.failure(HomeErrors.INVALID_COORDINATES);
        }

        try {
            return Result.success(new HomeLocation(cleanName, latitude, longitude, ZoneId.of(timeZone)));
        } catch (DateTimeException | NullPointerException exception) {
            return Result.failure(HomeErrors.INVALID_TIME_ZONE);
        }
    }

    private static boolean validCoordinates(double latitude, double longitude) {
        return Double.isFinite(latitude) && latitude >= -90 && latitude <= 90
            && Double.isFinite(longitude) && longitude >= -180 && longitude <= 180;
    }
}
