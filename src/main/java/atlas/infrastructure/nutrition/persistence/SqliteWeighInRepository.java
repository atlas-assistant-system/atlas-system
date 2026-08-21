package atlas.infrastructure.nutrition.persistence;

import atlas.application.nutrition.ports.WeighInRepository;
import atlas.domain.nutrition.WeighIn;
import atlas.domain.nutrition.WeighInId;
import atlas.infrastructure.nutrition.persistence.mappers.WeighInRows;
import atlas.infrastructure.sharedkernel.SequenceGenerator;
import atlas.infrastructure.sharedkernel.persistence.AbstractSqlRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public final class SqliteWeighInRepository extends AbstractSqlRepository<WeighIn, WeighInId>
    implements WeighInRepository {

    private static final String SEQUENCE = "weigh_ins";
    private static final String ON_DAY = "SELECT * FROM weigh_ins WHERE measured_on = ?";

    private final SequenceGenerator sequences;

    public SqliteWeighInRepository(Connection connection, SequenceGenerator sequences) {
        super(connection, "weigh_ins", "id");
        this.sequences = sequences;
    }

    @Override
    public WeighInId nextId() {
        return WeighInId.of(sequences.next(SEQUENCE));
    }

    @Override
    public Optional<WeighIn> findOn(LocalDate date) {
        return SqlQuery.list(
            connection(), ON_DAY,
            statement -> statement.setString(1, date.toString()),
            WeighInRows::toWeighIn)
            .stream()
            .findFirst();
    }

    @Override
    protected List<String> columns() {
        return List.of("id", "weight_g", "measured_on", "recorded_at");
    }

    @Override
    protected void bind(PreparedStatement statement, WeighIn weighIn) throws SQLException {
        WeighInRows.bind(statement, weighIn);
    }

    @Override
    protected WeighIn mapRow(ResultSet row) throws SQLException {
        return WeighInRows.toWeighIn(row);
    }

    @Override
    protected Object idValue(WeighInId id) {
        return id.value();
    }
}
