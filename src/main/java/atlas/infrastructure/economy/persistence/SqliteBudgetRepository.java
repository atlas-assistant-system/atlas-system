package atlas.infrastructure.economy.persistence;

import atlas.application.economy.ports.BudgetRepository;
import atlas.domain.economy.Budget;
import atlas.domain.economy.BudgetId;
import atlas.domain.economy.enums.Category;
import atlas.infrastructure.economy.persistence.mappers.BudgetRows;
import atlas.infrastructure.sharedkernel.SequenceGenerator;
import atlas.infrastructure.sharedkernel.persistence.AbstractSqlRepository;
import atlas.infrastructure.sharedkernel.persistence.PersistenceException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public final class SqliteBudgetRepository extends AbstractSqlRepository<Budget, BudgetId>
    implements BudgetRepository {

    private static final String SEQUENCE = "budgets";
    private static final String EXISTS_FOR = "SELECT 1 FROM budgets WHERE category = ?";

    private final Connection connection;
    private final SequenceGenerator sequences;

    public SqliteBudgetRepository(Connection connection, SequenceGenerator sequences) {
        super(connection, "budgets", "id");
        this.connection = connection;
        this.sequences = sequences;
    }

    @Override
    public BudgetId nextId() {
        return BudgetId.of(sequences.next(SEQUENCE));
    }

    @Override
    public boolean existsFor(Category category) {
        try (var statement = connection.prepareStatement(EXISTS_FOR)) {
            statement.setString(1, category.name());

            try (var rows = statement.executeQuery()) {
                return rows.next();
            }
        } catch (SQLException e) {
            throw new PersistenceException("Query failed: " + EXISTS_FOR, e);
        }
    }

    @Override
    protected List<String> columns() {
        return List.of("id", "category", "limit_cents", "currency");
    }

    @Override
    protected void bind(PreparedStatement statement, Budget budget) throws SQLException {
        BudgetRows.bind(statement, budget);
    }

    @Override
    protected Budget mapRow(ResultSet row) throws SQLException {
        return BudgetRows.toBudget(row);
    }

    @Override
    protected Object idValue(BudgetId id) {
        return id.value();
    }
}
