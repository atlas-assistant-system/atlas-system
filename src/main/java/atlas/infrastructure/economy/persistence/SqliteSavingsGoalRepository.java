package atlas.infrastructure.economy.persistence;

import atlas.application.economy.ports.SavingsGoalRepository;
import atlas.domain.economy.SavingsGoal;
import atlas.domain.economy.SavingsGoalId;
import atlas.infrastructure.economy.persistence.mappers.SavingsGoalRows;
import atlas.infrastructure.sharedkernel.SequenceGenerator;
import atlas.infrastructure.sharedkernel.persistence.AbstractSqlRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public final class SqliteSavingsGoalRepository extends AbstractSqlRepository<SavingsGoal, SavingsGoalId>
    implements SavingsGoalRepository {

    private static final String SEQUENCE = "savings_goals";

    private final SequenceGenerator sequences;

    public SqliteSavingsGoalRepository(Connection connection, SequenceGenerator sequences) {
        super(connection, "savings_goals", "id");
        this.sequences = sequences;
    }

    @Override
    public SavingsGoalId nextId() {
        return SavingsGoalId.of(sequences.next(SEQUENCE));
    }

    @Override
    protected List<String> columns() {
        return List.of("id", "name", "target_cents", "currency", "deadline");
    }

    @Override
    protected void bind(PreparedStatement statement, SavingsGoal goal) throws SQLException {
        SavingsGoalRows.bind(statement, goal);
    }

    @Override
    protected SavingsGoal mapRow(ResultSet row) throws SQLException {
        return SavingsGoalRows.toGoal(row);
    }

    @Override
    protected Object idValue(SavingsGoalId id) {
        return id.value();
    }
}
