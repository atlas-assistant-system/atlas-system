package atlas.infrastructure.nutrition.persistence.mappers;

import atlas.domain.nutrition.Plan;
import atlas.domain.nutrition.PlanId;
import atlas.domain.nutrition.enums.PlanStatus;
import atlas.domain.nutrition.vos.Macros;
import atlas.domain.nutrition.vos.Weight;
import atlas.domain.sharedkernel.results.Result;
import atlas.infrastructure.sharedkernel.persistence.PersistenceException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;

public final class PlanRows {

    private PlanRows() {}

    public static Plan toPlan(ResultSet row) throws SQLException {
        return Plan.rehydrate(
            PlanId.of(row.getLong("id")),
            weight(row.getInt("start_weight_g"), "plans.start_weight_g"),
            weight(row.getInt("target_weight_g"), "plans.target_weight_g"),
            macros(row),
            PlanStatus.valueOf(row.getString("status")),
            LocalDate.parse(row.getString("started_on")),
            Instant.parse(row.getString("defined_at")));
    }

    public static void bind(PreparedStatement statement, Plan plan) throws SQLException {
        var macros = plan.dailyMacros();

        statement.setLong(1, plan.id().value());
        statement.setInt(2, plan.startWeight().grams());
        statement.setInt(3, plan.targetWeight().grams());
        statement.setInt(4, macros.protein());
        statement.setInt(5, macros.carbs());
        statement.setInt(6, macros.fat());
        statement.setString(7, plan.status().name());
        statement.setString(8, plan.startedOn().toString());
        statement.setString(9, plan.definedAt().toString());
    }

    private static Weight weight(int grams, String column) {
        return require(Weight.ofGrams(grams), column);
    }

    private static Macros macros(ResultSet row) throws SQLException {
        return require(
            Macros.create(row.getInt("protein_g"), row.getInt("carbs_g"), row.getInt("fat_g")),
            "plans.protein_g/carbs_g/fat_g");
    }

    private static <T> T require(Result<T> result, String column) {
        if (result.isFailure()) {
            throw new PersistenceException("Corrupt value in " + column + ": " + result.error().message());
        }

        return result.value();
    }
}
