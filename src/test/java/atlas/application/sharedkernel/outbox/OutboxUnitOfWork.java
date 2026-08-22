package atlas.application.sharedkernel.outbox;

import atlas.application.sharedkernel.events.DomainEventSerializer;
import atlas.application.sharedkernel.unitofwork.AbstractUnitOfWork;
import atlas.infrastructure.sharedkernel.persistence.PersistenceException;
import atlas.infrastructure.sharedkernel.persistence.SqlOutboxStore;
import java.sql.Connection;
import java.sql.SQLException;

final class OutboxUnitOfWork extends AbstractUnitOfWork {

    private final Connection connection;

    OutboxUnitOfWork(
        Connection connection,
        SqlOutboxStore store,
        DomainEventSerializer serializer,
        OutboxProcessor processor) {
        super(new OutboxEventDelivery(store, serializer, processor));
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
