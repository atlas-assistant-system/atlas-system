package atlas.infrastructure.economy.persistence;

import atlas.application.economy.ports.MovementRepository;
import atlas.domain.economy.Movement;
import atlas.domain.economy.MovementId;
import atlas.infrastructure.economy.persistence.mappers.MovementRows;
import atlas.infrastructure.sharedkernel.SequenceGenerator;
import atlas.infrastructure.sharedkernel.persistence.AbstractSqlRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public final class SqliteMovementRepository extends AbstractSqlRepository<Movement, MovementId>
    implements MovementRepository {

    private static final String SEQUENCE = "movements";

    private final SequenceGenerator sequences;

    public SqliteMovementRepository(Connection connection, SequenceGenerator sequences) {
        super(connection, "movements", "id");
        this.sequences = sequences;
    }

    @Override
    public MovementId nextId() {
        return MovementId.of(sequences.next(SEQUENCE));
    }

    @Override
    protected List<String> columns() {
        return List.of("id", "amount_cents", "currency", "category", "note", "occurred_on", "recorded_at");
    }

    @Override
    protected void bind(PreparedStatement statement, Movement movement) throws SQLException {
        MovementRows.bind(statement, movement);
    }

    @Override
    protected Movement mapRow(ResultSet row) throws SQLException {
        return MovementRows.toMovement(row);
    }

    @Override
    protected Object idValue(MovementId id) {
        return id.value();
    }
}
