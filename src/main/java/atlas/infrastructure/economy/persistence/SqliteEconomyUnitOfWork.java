package atlas.infrastructure.economy.persistence;

import atlas.application.economy.ports.EconomyUnitOfWork;
import atlas.application.economy.ports.MovementRepository;
import atlas.application.sharedkernel.events.EventDelivery;
import atlas.application.sharedkernel.unitofwork.AbstractUnitOfWork;
import atlas.infrastructure.sharedkernel.SequenceGenerator;
import atlas.infrastructure.sharedkernel.persistence.PersistenceException;
import java.sql.Connection;
import java.sql.SQLException;

public final class SqliteEconomyUnitOfWork extends AbstractUnitOfWork implements EconomyUnitOfWork {

    private final Connection connection;
    private final SqliteMovementRepository movements;

    public SqliteEconomyUnitOfWork(Connection connection, EventDelivery delivery, SequenceGenerator sequences) {
        super(delivery);
        this.connection = connection;
        this.movements = new SqliteMovementRepository(connection, sequences);
    }

    @Override
    public MovementRepository movements() {
        return movements;
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
