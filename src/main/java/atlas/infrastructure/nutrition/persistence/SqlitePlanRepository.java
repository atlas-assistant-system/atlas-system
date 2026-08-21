package atlas.infrastructure.nutrition.persistence;

import atlas.application.nutrition.ports.PlanRepository;
import atlas.domain.nutrition.Plan;
import atlas.domain.nutrition.PlanId;
import atlas.domain.nutrition.enums.PlanStatus;
import atlas.infrastructure.nutrition.persistence.mappers.PlanRows;
import atlas.infrastructure.sharedkernel.SequenceGenerator;
import atlas.infrastructure.sharedkernel.persistence.AbstractSqlRepository;
import atlas.infrastructure.sharedkernel.persistence.PersistenceException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public final class SqlitePlanRepository extends AbstractSqlRepository<Plan, PlanId> implements PlanRepository {

    private static final String SEQUENCE = "plans";
    private static final String ACTIVE = "SELECT * FROM plans WHERE status = ?";

    private final SequenceGenerator sequences;

    public SqlitePlanRepository(Connection connection, SequenceGenerator sequences) {
        super(connection, "plans", "id");
        this.sequences = sequences;
    }

    @Override
    public PlanId nextId() {
        return PlanId.of(sequences.next(SEQUENCE));
    }

    @Override
    public Optional<Plan> findActive() {
        try (var statement = connection().prepareStatement(ACTIVE)) {
            statement.setString(1, PlanStatus.ACTIVE.name());

            try (var rows = statement.executeQuery()) {
                return rows.next() ? Optional.of(PlanRows.toPlan(rows)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenceException("Query failed: " + ACTIVE, e);
        }
    }

    @Override
    protected List<String> columns() {
        return List.of(
            "id", "start_weight_g", "target_weight_g", "protein_g", "carbs_g", "fat_g",
            "status", "started_on", "defined_at");
    }

    @Override
    protected void bind(PreparedStatement statement, Plan plan) throws SQLException {
        PlanRows.bind(statement, plan);
    }

    @Override
    protected Plan mapRow(ResultSet row) throws SQLException {
        return PlanRows.toPlan(row);
    }

    @Override
    protected Object idValue(PlanId id) {
        return id.value();
    }
}
