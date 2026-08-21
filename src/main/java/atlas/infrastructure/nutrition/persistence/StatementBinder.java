package atlas.infrastructure.nutrition.persistence;

import java.sql.PreparedStatement;
import java.sql.SQLException;

@FunctionalInterface
interface StatementBinder {

    void bind(PreparedStatement statement) throws SQLException;
}
