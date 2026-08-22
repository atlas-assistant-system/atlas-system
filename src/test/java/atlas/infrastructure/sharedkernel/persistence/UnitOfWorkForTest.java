package atlas.infrastructure.sharedkernel.persistence;

import atlas.application.sharedkernel.events.PendingEventDispatcher;
import atlas.application.sharedkernel.events.SimpleDomainEventPublisher;
import atlas.application.sharedkernel.unitofwork.AbstractUnitOfWork;
import java.sql.Connection;
import java.sql.SQLException;

final class UnitOfWorkForTest extends AbstractUnitOfWork {

    private final Connection connection;

    UnitOfWorkForTest(Connection connection, SimpleDomainEventPublisher publisher) {
        super(new PendingEventDispatcher(publisher));
        this.connection = connection;
    }

    @Override
    protected void begin() {
        onConnection(() -> connection.setAutoCommit(false));
    }

    @Override
    protected void commit() {
        onConnection(connection::commit);
    }

    @Override
    protected void rollback() {
        onConnection(connection::rollback);
    }

    private void onConnection(SqlAction action) {
        try {
            action.execute();
        } catch (SQLException e) {
            throw new PersistenceException("transaction failure", e);
        }
    }
}
