package atlas.infrastructure.nutrition.persistence;

import atlas.application.nutrition.ports.WeighInReadModel;
import atlas.domain.nutrition.WeighIn;
import atlas.domain.nutrition.WeighInId;
import atlas.infrastructure.nutrition.persistence.mappers.WeighInRows;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public final class SqliteWeighInReadModel implements WeighInReadModel {

    private static final String BY_ID = "SELECT * FROM weigh_ins WHERE id = ?";

    private static final String BETWEEN = """
        SELECT * FROM weigh_ins
        WHERE measured_on >= ? AND measured_on <= ?
        ORDER BY measured_on""";

    private static final String LATEST = "SELECT * FROM weigh_ins ORDER BY measured_on DESC LIMIT 1";

    private final Connection connection;

    public SqliteWeighInReadModel(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Optional<WeighIn> find(WeighInId id) {
        return query(BY_ID, statement -> statement.setLong(1, id.value())).stream().findFirst();
    }

    @Override
    public List<WeighIn> findBetween(LocalDate from, LocalDate to) {
        return query(BETWEEN, statement -> {
            statement.setString(1, from.toString());
            statement.setString(2, to.toString());
        });
    }

    @Override
    public Optional<WeighIn> findLatest() {
        return query(LATEST, statement -> {}).stream().findFirst();
    }

    private List<WeighIn> query(String sql, StatementBinder binder) {
        return SqlQuery.list(connection, sql, binder, WeighInRows::toWeighIn);
    }
}
