package atlas.infrastructure.presence.persistence;

import atlas.application.sharedkernel.events.EventDelivery;
import atlas.application.sharedkernel.events.PendingEventDispatcher;
import atlas.domain.presence.enums.VerificationOutcome;
import atlas.domain.presence.events.AuthenticationFailedEvent;
import atlas.domain.presence.events.AuthenticationSucceededEvent;
import atlas.domain.sharedkernel.ddd.AggregateRoot;
import atlas.infrastructure.sharedkernel.persistence.PersistenceException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public final class AuthenticationAuditDelivery implements EventDelivery {

    private final Connection connection;
    private final PendingEventDispatcher dispatcher;

    public AuthenticationAuditDelivery(Connection connection, PendingEventDispatcher dispatcher) {
        this.connection = connection;
        this.dispatcher = dispatcher;
    }

    @Override
    public void beforeCommit(List<? extends AggregateRoot<?>> changed) {
        var sql = "INSERT INTO authentication_attempts (occurred_on, outcome) VALUES (?, ?)";

        try (var statement = connection.prepareStatement(sql)) {
            for (var aggregate : changed) {
                for (var event : aggregate.pendingEvents()) {
                    if (event instanceof AuthenticationFailedEvent failed) {
                        statement.setString(1, failed.occurredOn().toString());
                        statement.setString(2, failed.outcome().name());
                        statement.addBatch();
                    } else if (event instanceof AuthenticationSucceededEvent succeeded) {
                        statement.setString(1, succeeded.occurredOn().toString());
                        statement.setString(2, VerificationOutcome.MATCHED.name());
                        statement.addBatch();
                    }
                }
            }
            statement.executeBatch();
        } catch (SQLException e) {
            throw new PersistenceException("Failed to append the authentication audit", e);
        }
    }

    @Override
    public void afterCommit(List<? extends AggregateRoot<?>> changed) {
        dispatcher.dispatch(changed);
    }
}
