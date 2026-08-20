package atlas.infrastructure.economy.persistence.mappers;

import atlas.domain.economy.Budget;
import atlas.domain.economy.BudgetId;
import atlas.domain.economy.enums.Category;
import atlas.domain.economy.vos.Money;
import atlas.domain.sharedkernel.results.Result;
import atlas.infrastructure.sharedkernel.persistence.PersistenceException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class BudgetRows {

    private BudgetRows() {}

    public static Budget toBudget(ResultSet row) throws SQLException {
        return Budget.rehydrate(
            BudgetId.of(row.getLong("id")),
            Category.valueOf(row.getString("category")),
            require(Money.ofCents(row.getLong("limit_cents")), "budgets.limit_cents"));
    }

    public static void bind(PreparedStatement statement, Budget budget) throws SQLException {
        statement.setLong(1, budget.id().value());
        statement.setString(2, budget.category().name());
        statement.setLong(3, budget.limit().cents());
        statement.setString(4, budget.limit().currency());
    }

    private static <T> T require(Result<T> result, String column) {
        if (result.isFailure()) {
            throw new PersistenceException("Corrupt value in " + column + ": " + result.error().message());
        }

        return result.value();
    }
}
