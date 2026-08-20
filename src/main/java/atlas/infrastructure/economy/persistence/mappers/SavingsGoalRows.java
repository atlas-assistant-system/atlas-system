package atlas.infrastructure.economy.persistence.mappers;

import atlas.domain.economy.SavingsGoal;
import atlas.domain.economy.SavingsGoalId;
import atlas.domain.economy.vos.GoalName;
import atlas.domain.economy.vos.Money;
import atlas.domain.sharedkernel.results.Result;
import atlas.infrastructure.sharedkernel.persistence.PersistenceException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

public final class SavingsGoalRows {

    private SavingsGoalRows() {}

    public static SavingsGoal toGoal(ResultSet row) throws SQLException {
        return SavingsGoal.rehydrate(
            SavingsGoalId.of(row.getLong("id")),
            require(GoalName.create(row.getString("name")), "savings_goals.name"),
            require(Money.ofCents(row.getLong("target_cents")), "savings_goals.target_cents"),
            LocalDate.parse(row.getString("deadline")));
    }

    public static void bind(PreparedStatement statement, SavingsGoal goal) throws SQLException {
        statement.setLong(1, goal.id().value());
        statement.setString(2, goal.name().value());
        statement.setLong(3, goal.target().cents());
        statement.setString(4, goal.target().currency());
        statement.setString(5, goal.deadline().toString());
    }

    private static <T> T require(Result<T> result, String column) {
        if (result.isFailure()) {
            throw new PersistenceException("Corrupt value in " + column + ": " + result.error().message());
        }

        return result.value();
    }
}
