package atlas.infrastructure.training.persistence.mappers;

import atlas.domain.training.vos.Effort;
import atlas.infrastructure.sharedkernel.persistence.PersistenceException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Optional;

/**
 * Las cuatro columnas de un Effort van juntas: o las cuatro con valor o las cuatro NULL.
 * NULL significa "esta serie no tiene plan" o "esta serie sigue pendiente". No hay columna
 * de bandera aparte porque permitiria que bandera y valores discrepasen.
 */
public final class EffortRows {

    private EffortRows() {}

    public static Effort required(ResultSet row, String prefix) throws SQLException {
        return read(row, prefix).orElseThrow(
            () -> new PersistenceException("Missing effort in " + prefix));
    }

    public static Optional<Effort> read(ResultSet row, String prefix) throws SQLException {
        var loadGrams = row.getInt(prefix + "load_g");
        if (row.wasNull()) {
            return Optional.empty();
        }

        var effort = Effort.create(
            loadGrams,
            row.getInt(prefix + "reps"),
            row.getInt(prefix + "seconds"),
            row.getInt(prefix + "meters"));
        if (effort.isFailure()) {
            throw new PersistenceException(
                "Corrupt effort in " + prefix + ": " + effort.error().message());
        }

        return Optional.of(effort.value());
    }

    public static void bind(PreparedStatement statement, int from, Optional<Effort> effort)
        throws SQLException {

        if (effort.isEmpty()) {
            for (var offset = 0; offset < 4; offset++) {
                statement.setNull(from + offset, Types.INTEGER);
            }

            return;
        }

        var value = effort.get();
        statement.setInt(from, value.loadGrams());
        statement.setInt(from + 1, value.reps());
        statement.setInt(from + 2, value.seconds());
        statement.setInt(from + 3, value.meters());
    }
}
