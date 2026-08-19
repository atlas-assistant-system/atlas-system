package atlas.infrastructure.sharedkernel.persistence;

import atlas.application.sharedkernel.ports.Repository;
import atlas.application.sharedkernel.unitofwork.AggregateChanges;
import atlas.domain.sharedkernel.ddd.AggregateRoot;
import atlas.domain.sharedkernel.guards.ObjectGuard;
import atlas.domain.sharedkernel.guards.StringGuard;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public abstract class AbstractSqlRepository<T extends AggregateRoot<TId>, TId> implements Repository<T, TId> {

    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    private final Connection connection;
    private final String tableName;
    private final String idColumn;

    protected AbstractSqlRepository(Connection connection, String tableName, String idColumn) {
        this.connection = ObjectGuard.notNull(connection, "connection");
        this.tableName = requireSafeIdentifier(tableName, "tableName");
        this.idColumn = requireSafeIdentifier(idColumn, "idColumn");
    }

    protected abstract List<String> columns();

    protected abstract void bind(PreparedStatement statement, T aggregate) throws SQLException;

    protected abstract T mapRow(ResultSet row) throws SQLException;

    protected abstract Object idValue(TId id);

    @Override
    public Optional<T> get(TId id) {
        ObjectGuard.notNull(id, "id");

        var sql = "SELECT * FROM " + tableName + " WHERE " + idColumn + " = ?";

        try (var statement = connection.prepareStatement(sql)) {
            statement.setObject(1, idValue(id));

            try (var rows = statement.executeQuery()) {
                return rows.next() ? Optional.of(mapRow(rows)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenceException("Failed to read from " + tableName, e);
        }
    }

    @Override
    public List<T> getAll() {
        var sql = "SELECT * FROM " + tableName;
        var found = new ArrayList<T>();

        try (var statement = connection.prepareStatement(sql); var rows = statement.executeQuery()) {
            while (rows.next()) {
                found.add(mapRow(rows));
            }

            return List.copyOf(found);
        } catch (SQLException e) {
            throw new PersistenceException("Failed to read from " + tableName, e);
        }
    }

    @Override
    public void create(T aggregate) {
        ObjectGuard.notNull(aggregate, "aggregate");

        var names = safeColumns();
        var sql = "INSERT INTO " + tableName + " (" + String.join(", ", names) + ") VALUES ("
            + String.join(", ", placeholders(names.size())) + ")";

        execute(sql, aggregate, false, "insert into " + tableName);
    }

    @Override
    public void update(T aggregate) {
        ObjectGuard.notNull(aggregate, "aggregate");

        var names = safeColumns();
        var assignments = new ArrayList<String>(names.size());
        for (var column : names) {
            assignments.add(column + " = ?");
        }

        var sql = "UPDATE " + tableName + " SET " + String.join(", ", assignments)
            + " WHERE " + idColumn + " = ?";

        execute(sql, aggregate, true, "update " + tableName);
    }

    @Override
    public void delete(T aggregate) {
        ObjectGuard.notNull(aggregate, "aggregate");

        var sql = "DELETE FROM " + tableName + " WHERE " + idColumn + " = ?";

        try (var statement = connection.prepareStatement(sql)) {
            statement.setObject(1, idValue(aggregate.id()));
            requireAffectedRow(statement.executeUpdate(), "delete from " + tableName, aggregate);
        } catch (SQLException e) {
            throw new PersistenceException("Failed to delete from " + tableName, e);
        }

        AggregateChanges.track(aggregate);
    }

    protected final Connection connection() {
        return connection;
    }

    private void execute(String sql, T aggregate, boolean bindIdAtEnd, String operation) {
        try (var statement = connection.prepareStatement(sql)) {
            bind(statement, aggregate);

            if (bindIdAtEnd) {
                statement.setObject(columns().size() + 1, idValue(aggregate.id()));
            }

            requireAffectedRow(statement.executeUpdate(), operation, aggregate);
        } catch (SQLException e) {
            throw new PersistenceException("Failed to " + operation, e);
        }

        AggregateChanges.track(aggregate);
    }

    private void requireAffectedRow(int affected, String operation, T aggregate) {
        if (affected == 0) {
            throw new PersistenceException("No row affected by " + operation + " for id " + aggregate.id());
        }
    }

    private List<String> safeColumns() {
        var declared = columns();

        if (declared == null || declared.isEmpty()) {
            throw new PersistenceException("columns() must declare at least one column for " + tableName);
        }

        var checked = new ArrayList<String>(declared.size());
        for (var column : declared) {
            checked.add(requireSafeIdentifier(column, "column"));
        }

        return checked;
    }

    private static List<String> placeholders(int count) {
        var marks = new ArrayList<String>(count);
        for (int i = 0; i < count; i++) {
            marks.add("?");
        }

        return marks;
    }

    private static String requireSafeIdentifier(String identifier, String parameterName) {
        StringGuard.notBlank(identifier, parameterName);

        if (!SAFE_IDENTIFIER.matcher(identifier).matches()) {
            throw new PersistenceException("Unsafe SQL identifier for " + parameterName + ": " + identifier);
        }

        return identifier;
    }
}
