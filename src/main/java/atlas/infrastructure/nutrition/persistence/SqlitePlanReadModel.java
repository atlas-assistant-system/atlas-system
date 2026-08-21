package atlas.infrastructure.nutrition.persistence;

import atlas.application.nutrition.ports.PlanReadModel;
import atlas.domain.nutrition.Plan;
import atlas.domain.nutrition.enums.PlanStatus;
import atlas.infrastructure.nutrition.persistence.mappers.PlanRows;
import java.sql.Connection;
import java.util.Optional;

public final class SqlitePlanReadModel implements PlanReadModel {

    private static final String ACTIVE = "SELECT * FROM plans WHERE status = ?";

    private final Connection connection;

    public SqlitePlanReadModel(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Optional<Plan> findActive() {
        return SqlQuery.list(
            connection, ACTIVE,
            statement -> statement.setString(1, PlanStatus.ACTIVE.name()),
            PlanRows::toPlan)
            .stream()
            .findFirst();
    }
}
