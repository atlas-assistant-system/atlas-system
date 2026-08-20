package atlas.infrastructure.economy.persistence;

import atlas.application.economy.ports.BudgetReadModel;
import atlas.domain.economy.Budget;
import atlas.infrastructure.economy.persistence.mappers.BudgetRows;
import atlas.infrastructure.sharedkernel.persistence.PersistenceException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class SqliteBudgetReadModel implements BudgetReadModel {

    private static final String ALL = "SELECT * FROM budgets ORDER BY category";

    private final Connection connection;

    public SqliteBudgetReadModel(Connection connection) {
        this.connection = connection;
    }

    @Override
    public List<Budget> findAll() {
        try (var statement = connection.prepareStatement(ALL); var rows = statement.executeQuery()) {
            var budgets = new ArrayList<Budget>();

            while (rows.next()) {
                budgets.add(BudgetRows.toBudget(rows));
            }

            return List.copyOf(budgets);
        } catch (SQLException e) {
            throw new PersistenceException("Query failed: " + ALL, e);
        }
    }
}
