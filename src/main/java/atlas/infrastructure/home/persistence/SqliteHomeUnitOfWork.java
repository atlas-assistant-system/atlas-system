package atlas.infrastructure.home.persistence;

import atlas.application.home.ports.HomeProfileRepository;
import atlas.application.home.ports.HomeUnitOfWork;
import atlas.application.sharedkernel.events.EventDelivery;
import atlas.application.sharedkernel.unitofwork.AbstractUnitOfWork;
import atlas.infrastructure.sharedkernel.persistence.PersistenceException;
import java.sql.Connection;
import java.sql.SQLException;

public final class SqliteHomeUnitOfWork extends AbstractUnitOfWork implements HomeUnitOfWork {

    private final Connection connection;
    private final HomeProfileRepository profiles;

    public SqliteHomeUnitOfWork(Connection connection, EventDelivery delivery) {
        super(delivery);
        this.connection = connection;
        profiles = new SqliteHomeProfileRepository(connection);
    }

    @Override
    public HomeProfileRepository profiles() {
        return profiles;
    }

    @Override
    protected void begin() {
        try {
            connection.setAutoCommit(false);
        } catch (SQLException exception) {
            throw new PersistenceException("Failed to begin a transaction", exception);
        }
    }

    @Override
    protected void commit() {
        try {
            connection.commit();
        } catch (SQLException exception) {
            throw new PersistenceException("Failed to commit the transaction", exception);
        }
    }

    @Override
    protected void rollback() {
        try {
            connection.rollback();
        } catch (SQLException exception) {
            throw new PersistenceException("Failed to roll back the transaction", exception);
        }
    }
}
