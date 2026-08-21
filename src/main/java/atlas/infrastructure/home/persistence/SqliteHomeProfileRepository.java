package atlas.infrastructure.home.persistence;

import atlas.application.home.ports.HomeProfileRepository;
import atlas.domain.home.HomeProfile;
import atlas.domain.home.HomeProfileId;
import atlas.infrastructure.home.persistence.mappers.HomeRows;
import atlas.infrastructure.sharedkernel.persistence.AbstractSqlRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public final class SqliteHomeProfileRepository extends AbstractSqlRepository<HomeProfile, HomeProfileId>
    implements HomeProfileRepository {

    public SqliteHomeProfileRepository(Connection connection) {
        super(connection, "home_profiles", "profile_id");
    }

    @Override
    protected List<String> columns() {
        return List.of(
            "profile_id", "location_name", "latitude", "longitude", "time_zone", "news_categories");
    }

    @Override
    protected void bind(PreparedStatement statement, HomeProfile profile) throws SQLException {
        HomeRows.bind(statement, profile);
    }

    @Override
    protected HomeProfile mapRow(ResultSet row) throws SQLException {
        return HomeRows.toProfile(row);
    }

    @Override
    protected Object idValue(HomeProfileId id) {
        return id.value();
    }
}
