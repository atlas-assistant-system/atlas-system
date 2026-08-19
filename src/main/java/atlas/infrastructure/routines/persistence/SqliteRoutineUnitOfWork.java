package atlas.infrastructure.routines.persistence;

import atlas.application.routines.ports.RoutineEntryRepository;
import atlas.application.routines.ports.RoutineRepository;
import atlas.application.routines.ports.RoutineUnitOfWork;
import java.sql.Connection;
import java.sql.SQLException;
import sharedkernel.application.events.EventDelivery;
import sharedkernel.application.unitofwork.AbstractUnitOfWork;
import sharedkernel.infrastructure.SequenceGenerator;
import sharedkernel.infrastructure.persistence.PersistenceException;

public final class SqliteRoutineUnitOfWork extends AbstractUnitOfWork implements RoutineUnitOfWork {

    private final Connection connection;
    private final SqliteRoutineRepository routines;
    private final SqliteRoutineEntryRepository entries;

    public SqliteRoutineUnitOfWork(Connection connection, EventDelivery delivery, SequenceGenerator sequences) {
        super(delivery);
        this.connection = connection;
        this.routines = new SqliteRoutineRepository(connection, sequences);
        this.entries = new SqliteRoutineEntryRepository(connection);
    }

    @Override
    public RoutineRepository routines() {
        return routines;
    }

    @Override
    public RoutineEntryRepository entries() {
        return entries;
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
