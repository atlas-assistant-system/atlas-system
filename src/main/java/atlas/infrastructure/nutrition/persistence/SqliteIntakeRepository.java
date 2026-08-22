package atlas.infrastructure.nutrition.persistence;

import atlas.application.nutrition.ports.IntakeRepository;
import atlas.domain.nutrition.Intake;
import atlas.domain.nutrition.IntakeId;
import atlas.infrastructure.nutrition.persistence.mappers.IntakeRows;
import atlas.infrastructure.sharedkernel.SequenceGenerator;
import atlas.infrastructure.sharedkernel.persistence.AbstractSqlRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public final class SqliteIntakeRepository extends AbstractSqlRepository<Intake, IntakeId>
    implements IntakeRepository {

    private static final String SEQUENCE = "intakes";

    private final SequenceGenerator sequences;

    public SqliteIntakeRepository(Connection connection, SequenceGenerator sequences) {
        super(connection, "intakes", "id");
        this.sequences = sequences;
    }

    @Override
    public IntakeId nextId() {
        return IntakeId.of(sequences.next(SEQUENCE));
    }

    @Override
    protected List<String> columns() {
        return List.of(
            "id", "calories", "protein_g", "carbs_g", "fat_g", "note", "consumed_on", "recorded_at");
    }

    @Override
    protected void bind(PreparedStatement statement, Intake intake) throws SQLException {
        IntakeRows.bind(statement, intake);
    }

    @Override
    protected Intake mapRow(ResultSet row) throws SQLException {
        return IntakeRows.toIntake(row);
    }

    @Override
    protected Object idValue(IntakeId id) {
        return id.value();
    }
}
