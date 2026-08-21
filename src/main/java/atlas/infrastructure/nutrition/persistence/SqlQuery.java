package atlas.infrastructure.nutrition.persistence;

import atlas.infrastructure.sharedkernel.persistence.PersistenceException;
import atlas.infrastructure.sharedkernel.persistence.RowMapper;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

final class SqlQuery {

    private SqlQuery() {}

    static <T> List<T> list(Connection connection, String sql, StatementBinder binder, RowMapper<T> mapper) {
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
}
