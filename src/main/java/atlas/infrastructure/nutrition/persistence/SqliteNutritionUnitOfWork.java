package atlas.infrastructure.nutrition.persistence;

import atlas.application.nutrition.ports.IntakeRepository;
import atlas.application.nutrition.ports.NutritionUnitOfWork;
import atlas.application.nutrition.ports.PlanRepository;
import atlas.application.nutrition.ports.WeighInRepository;
import atlas.application.sharedkernel.events.EventDelivery;
import atlas.application.sharedkernel.unitofwork.AbstractUnitOfWork;
import atlas.infrastructure.sharedkernel.SequenceGenerator;
import atlas.infrastructure.sharedkernel.persistence.PersistenceException;
import java.sql.Connection;
import java.sql.SQLException;

public final class SqliteNutritionUnitOfWork extends AbstractUnitOfWork implements NutritionUnitOfWork {

    private final Connection connection;
    private final SqlitePlanRepository plans;
    private final SqliteIntakeRepository intakes;
    private final SqliteWeighInRepository weighIns;

    public SqliteNutritionUnitOfWork(
        Connection connection, EventDelivery delivery, SequenceGenerator sequences) {
        super(delivery);
        this.connection = connection;
        this.plans = new SqlitePlanRepository(connection, sequences);
        this.intakes = new SqliteIntakeRepository(connection, sequences);
        this.weighIns = new SqliteWeighInRepository(connection, sequences);
    }

    @Override
    public PlanRepository plans() {
        return plans;
    }

    @Override
    public IntakeRepository intakes() {
        return intakes;
    }

    @Override
    public WeighInRepository weighIns() {
        return weighIns;
    }

    @Override
    protected void begin() {
        try {
            connection.setAutoCommit(false);
        } catch (SQLException e) {
            throw new PersistenceException("Failed to begin a transaction", e);
        }
    }

    @Override
    protected void commit() {
        try {
            connection.commit();
        } catch (SQLException e) {
            throw new PersistenceException("Failed to commit the transaction", e);
        }
    }

    @Override
    protected void rollback() {
        try {
            connection.rollback();
        } catch (SQLException e) {
            throw new PersistenceException("Failed to roll back the transaction", e);
        }
    }
}
