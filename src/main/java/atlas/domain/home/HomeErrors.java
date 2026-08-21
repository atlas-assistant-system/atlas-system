package atlas.domain.home;

import atlas.domain.sharedkernel.results.Error;

public final class HomeErrors {

    public static final Error LOCATION_NAME_REQUIRED =
        Error.validation("Home.LocationNameRequired", "A location name is required.");

    public static final Error LOCATION_NAME_TOO_LONG =
        Error.validation("Home.LocationNameTooLong", "The location name is too long.");

    public static final Error INVALID_COORDINATES =
        Error.validation("Home.InvalidCoordinates", "The location coordinates are invalid.");

    public static final Error INVALID_TIME_ZONE =
        Error.validation("Home.InvalidTimeZone", "The location time zone is invalid.");

    public static final Error NEWS_CATEGORIES_REQUIRED =
        Error.validation("Home.NewsCategoriesRequired", "At least one news category is required.");

    public static final Error ACCESS_DENIED =
        Error.forbidden("Home.AccessDenied", "The home profile belongs to another user.");

    public static Error notFound(HomeProfileId id) {
        return Error.notFound("Home.ProfileNotFound", "Home profile '" + id + "' was not found.");
    }

    private HomeErrors() {}
}
