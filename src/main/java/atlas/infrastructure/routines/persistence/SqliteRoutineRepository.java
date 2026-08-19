package atlas.infrastructure.routines.persistence;

import atlas.application.routines.ports.RoutineRepository;
import atlas.domain.routines.Routine;
import atlas.domain.routines.RoutineId;
import atlas.infrastructure.routines.persistence.mappers.RoutineRows;
import atlas.infrastructure.sharedkernel.SequenceGenerator;
import atlas.infrastructure.sharedkernel.persistence.AbstractSqlRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public final class SqliteRoutineRepository extends AbstractSqlRepository<Routine, RoutineId>
    implements RoutineRepository {

    private static final String SEQUENCE = "routines";

    private final SequenceGenerator sequences;

    public SqliteRoutineRepository(Connection connection, SequenceGenerator sequences) {
        super(connection, "routines", "id");
        this.sequences = sequences;
    }

    @Override
    public RoutineId nextId() {
        return RoutineId.of(sequences.next(SEQUENCE));
    }

    @Override
    protected List<String> columns() {
        return List.of(
            "id",
            "name",
            "description",
            "target_amount",
            "target_unit",
            "period",
            "active_days",
            "days_of_month",
            "archived");
    }

    @Override
    protected void bind(PreparedStatement statement, Routine routine) throws SQLException {
        RoutineRows.bind(statement, routine);
    }

    @Override
    protected Routine mapRow(ResultSet row) throws SQLException {
        return RoutineRows.toRoutine(row);
    }

    @Override
    protected Object idValue(RoutineId id) {
        return id.value();
    }
}
