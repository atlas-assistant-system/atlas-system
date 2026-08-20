package atlas.infrastructure.economy.persistence;

import atlas.application.economy.ports.SavingsGoalReadModel;
import atlas.domain.economy.SavingsGoal;
import atlas.infrastructure.economy.persistence.mappers.SavingsGoalRows;
import atlas.infrastructure.sharedkernel.persistence.PersistenceException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class SqliteSavingsGoalReadModel implements SavingsGoalReadModel {

    private static final String ALL = "SELECT * FROM savings_goals ORDER BY deadline";

    private final Connection connection;

    public SqliteSavingsGoalReadModel(Connection connection) {
        this.connection = connection;
    }

    @Override
    public List<SavingsGoal> findAll() {
        try (var statement = connection.prepareStatement(ALL); var rows = statement.executeQuery()) {
            var goals = new ArrayList<SavingsGoal>();

            while (rows.next()) {
                goals.add(SavingsGoalRows.toGoal(rows));
            }

            return List.copyOf(goals);
        } catch (SQLException e) {
            throw new PersistenceException("Query failed: " + ALL, e);
        }
    }
}
