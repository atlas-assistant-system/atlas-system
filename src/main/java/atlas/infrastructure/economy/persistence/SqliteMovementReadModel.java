package atlas.infrastructure.economy.persistence;

import atlas.application.economy.ports.MovementReadModel;
import atlas.domain.economy.Movement;
import atlas.domain.economy.MovementId;
import atlas.domain.economy.enums.Category;
import atlas.infrastructure.economy.persistence.mappers.MovementRows;
import atlas.infrastructure.sharedkernel.persistence.PersistenceException;
import atlas.infrastructure.sharedkernel.persistence.RowMapper;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class SqliteMovementReadModel implements MovementReadModel {

    private static final String BY_ID = "SELECT * FROM movements WHERE id = ?";

    private static final String BALANCE = """
        SELECT
            COALESCE(SUM(CASE WHEN amount_cents > 0 THEN amount_cents END), 0)  AS income_cents,
            COALESCE(-SUM(CASE WHEN amount_cents < 0 THEN amount_cents END), 0) AS expense_cents
        FROM movements
        WHERE occurred_on >= ? AND occurred_on <= ?""";

    private static final String SPENDING_BY_CATEGORY = """
        SELECT category, -SUM(amount_cents) AS cents
        FROM movements
        WHERE amount_cents < 0 AND occurred_on >= ? AND occurred_on <= ?
        GROUP BY category""";

    private final Connection connection;

    public SqliteMovementReadModel(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Optional<Movement> find(MovementId id) {
        return query(BY_ID, statement -> statement.setLong(1, id.value()), MovementRows::toMovement)
            .stream()
            .findFirst();
    }

    @Override
    public List<Movement> findLatest(LocalDate from, LocalDate to, Category category, int limit) {
        var sql = new StringBuilder("SELECT * FROM movements WHERE 1 = 1");
        var filters = new ArrayList<String>();

        if (from != null) {
            sql.append(" AND occurred_on >= ?");
            filters.add(from.toString());
        }

        if (to != null) {
            sql.append(" AND occurred_on <= ?");
            filters.add(to.toString());
        }

        if (category != null) {
            sql.append(" AND category = ?");
            filters.add(category.name());
        }

        sql.append(" ORDER BY occurred_on DESC, id DESC LIMIT ?");

        return query(sql.toString(), statement -> {
            var index = 1;
            for (var filter : filters) {
                statement.setString(index++, filter);
            }
            statement.setInt(index, limit);
        }, MovementRows::toMovement);
    }

    @Override
    public Balance balanceBetween(LocalDate from, LocalDate to) {
        return query(BALANCE, between(from, to),
            row -> new Balance(row.getLong("income_cents"), row.getLong("expense_cents")))
            .getFirst();
    }

    @Override
    public List<CategorySpend> spendingBetween(LocalDate from, LocalDate to) {
        return query(SPENDING_BY_CATEGORY, between(from, to),
            row -> new CategorySpend(Category.valueOf(row.getString("category")), row.getLong("cents")));
    }

    private static Binder between(LocalDate from, LocalDate to) {
        return statement -> {
            statement.setString(1, from.toString());
            statement.setString(2, to.toString());
        };
    }

    private <T> List<T> query(String sql, Binder binder, RowMapper<T> mapper) {
        try (var statement = connection.prepareStatement(sql)) {
            binder.bind(statement);

            try (var rows = statement.executeQuery()) {
                var result = new ArrayList<T>();

                while (rows.next()) {
                    result.add(mapper.map(rows));
                }

                return List.copyOf(result);
            }
        } catch (SQLException e) {
            throw new PersistenceException("Query failed: " + sql, e);
        }
    }

    @FunctionalInterface
    private interface Binder {

        void bind(PreparedStatement statement) throws SQLException;
    }
}
