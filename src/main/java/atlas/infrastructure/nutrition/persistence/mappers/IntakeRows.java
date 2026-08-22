package atlas.infrastructure.nutrition.persistence.mappers;

import atlas.domain.nutrition.Intake;
import atlas.domain.nutrition.IntakeId;
import atlas.domain.nutrition.vos.Calories;
import atlas.domain.nutrition.vos.IntakeNote;
import atlas.domain.nutrition.vos.Macros;
import atlas.domain.sharedkernel.results.Result;
import atlas.infrastructure.sharedkernel.persistence.PersistenceException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;

public final class IntakeRows {

    private IntakeRows() {}

    public static Intake toIntake(ResultSet row) throws SQLException {
        return Intake.rehydrate(
            IntakeId.of(row.getLong("id")),
            calories(row),
            macros(row),
            require(IntakeNote.create(row.getString("note")), "intakes.note"),
            LocalDate.parse(row.getString("consumed_on")),
            Instant.parse(row.getString("recorded_at")));
    }

    public static Calories calories(ResultSet row) throws SQLException {
        return require(Calories.create(row.getInt("calories")), "intakes.calories");
    }

    public static Macros macros(ResultSet row) throws SQLException {
        return require(
            Macros.create(row.getInt("protein_g"), row.getInt("carbs_g"), row.getInt("fat_g")),
            "intakes.protein_g/carbs_g/fat_g");
    }

    public static void bind(PreparedStatement statement, Intake intake) throws SQLException {
        var macros = intake.macros();

        statement.setLong(1, intake.id().value());
        statement.setInt(2, intake.calories().kcal());
        statement.setInt(3, macros.protein());
        statement.setInt(4, macros.carbs());
        statement.setInt(5, macros.fat());
        statement.setString(6, intake.note().map(IntakeNote::value).orElse(null));
        statement.setString(7, intake.consumedOn().toString());
        statement.setString(8, intake.recordedAt().toString());
    }

    private static <T> T require(Result<T> result, String column) {
        if (result.isFailure()) {
            throw new PersistenceException("Corrupt value in " + column + ": " + result.error().message());
        }

        return result.value();
    }
}
