package atlas.infrastructure.nutrition.persistence.mappers;

import atlas.domain.nutrition.WeighIn;
import atlas.domain.nutrition.WeighInId;
import atlas.domain.nutrition.vos.Weight;
import atlas.domain.sharedkernel.results.Result;
import atlas.infrastructure.sharedkernel.persistence.PersistenceException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;

public final class WeighInRows {

    private WeighInRows() {}

    public static WeighIn toWeighIn(ResultSet row) throws SQLException {
        return WeighIn.rehydrate(
            WeighInId.of(row.getLong("id")),
            require(Weight.ofGrams(row.getInt("weight_g")), "weigh_ins.weight_g"),
            LocalDate.parse(row.getString("measured_on")),
            Instant.parse(row.getString("recorded_at")));
    }

    public static void bind(PreparedStatement statement, WeighIn weighIn) throws SQLException {
        statement.setLong(1, weighIn.id().value());
        statement.setInt(2, weighIn.weight().grams());
        statement.setString(3, weighIn.measuredOn().toString());
        statement.setString(4, weighIn.recordedAt().toString());
    }

    private static <T> T require(Result<T> result, String column) {
        if (result.isFailure()) {
            throw new PersistenceException("Corrupt value in " + column + ": " + result.error().message());
        }

        return result.value();
    }
}
