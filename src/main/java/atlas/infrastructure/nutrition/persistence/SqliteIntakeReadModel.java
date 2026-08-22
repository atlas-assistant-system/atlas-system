package atlas.infrastructure.nutrition.persistence;

import atlas.application.nutrition.ports.DayConsumption;
import atlas.application.nutrition.ports.IntakeReadModel;
import atlas.domain.nutrition.Intake;
import atlas.domain.nutrition.IntakeId;
import atlas.infrastructure.nutrition.persistence.mappers.IntakeRows;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public final class SqliteIntakeReadModel implements IntakeReadModel {

    private static final String BY_ID = "SELECT * FROM intakes WHERE id = ?";

    private static final String ON_DAY = """
        SELECT * FROM intakes
        WHERE consumed_on = ?
        ORDER BY recorded_at, id""";

    private static final String BETWEEN = """
        SELECT * FROM intakes
        WHERE consumed_on >= ? AND consumed_on <= ?
        ORDER BY consumed_on DESC, id DESC
        LIMIT ?""";

    private static final String CONSUMPTION_BY_DAY = """
        SELECT consumed_on,
               SUM(calories)  AS calories,
               SUM(protein_g) AS protein_g,
               SUM(carbs_g)   AS carbs_g,
               SUM(fat_g)     AS fat_g
        FROM intakes
        WHERE consumed_on >= ? AND consumed_on <= ?
        GROUP BY consumed_on""";

    private final Connection connection;

    public SqliteIntakeReadModel(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Optional<Intake> find(IntakeId id) {
        return query(BY_ID, statement -> statement.setLong(1, id.value())).stream().findFirst();
    }

    @Override
    public List<Intake> findOn(LocalDate date) {
        return query(ON_DAY, statement -> statement.setString(1, date.toString()));
    }

    @Override
    public List<Intake> findBetween(LocalDate from, LocalDate to, int limit) {
        return query(BETWEEN, statement -> {
            statement.setString(1, from.toString());
            statement.setString(2, to.toString());
            statement.setInt(3, limit);
        });
    }

    @Override
    public List<DayConsumption> consumptionBetween(LocalDate from, LocalDate to) {
        return SqlQuery.list(connection, CONSUMPTION_BY_DAY, statement -> {
            statement.setString(1, from.toString());
            statement.setString(2, to.toString());
        }, row -> new DayConsumption(
            LocalDate.parse(row.getString("consumed_on")),
            IntakeRows.calories(row),
            IntakeRows.macros(row)));
    }

    private List<Intake> query(String sql, StatementBinder binder) {
        return SqlQuery.list(connection, sql, binder, IntakeRows::toIntake);
    }
}
