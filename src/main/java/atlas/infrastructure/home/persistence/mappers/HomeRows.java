package atlas.infrastructure.home.persistence.mappers;

import atlas.domain.home.HomeProfile;
import atlas.domain.home.HomeProfileId;
import atlas.domain.home.enums.NewsCategory;
import atlas.domain.home.vos.HomeLocation;
import atlas.domain.sharedkernel.results.Result;
import atlas.infrastructure.sharedkernel.persistence.PersistenceException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public final class HomeRows {

    private HomeRows() {}

    public static HomeProfile toProfile(ResultSet row) throws SQLException {
        var location = require(HomeLocation.create(
            row.getString("location_name"),
            row.getDouble("latitude"),
            row.getDouble("longitude"),
            row.getString("time_zone")), "home_profiles.location");

        return HomeProfile.rehydrate(
            HomeProfileId.of(row.getString("profile_id")),
            location,
            categories(row.getString("news_categories")));
    }

    public static void bind(PreparedStatement statement, HomeProfile profile) throws SQLException {
        var location = profile.location();
        statement.setString(1, profile.id().value());
        statement.setString(2, location.name());
        statement.setDouble(3, location.latitude());
        statement.setDouble(4, location.longitude());
        statement.setString(5, location.timeZone().getId());
        statement.setString(6, profile.newsCategories().stream()
            .sorted()
            .map(NewsCategory::name)
            .collect(Collectors.joining(",")));
    }

    private static Set<NewsCategory> categories(String stored) {
        try {
            return Arrays.stream(stored.split(","))
                .filter(value -> !value.isBlank())
                .map(NewsCategory::valueOf)
                .collect(Collectors.toUnmodifiableSet());
        } catch (RuntimeException exception) {
            throw new PersistenceException("Corrupt value in home_profiles.news_categories", exception);
        }
    }

    private static <T> T require(Result<T> result, String column) {
        if (result.isFailure()) {
            throw new PersistenceException("Corrupt value in " + column + ": " + result.error().message());
        }

        return result.value();
    }
}
