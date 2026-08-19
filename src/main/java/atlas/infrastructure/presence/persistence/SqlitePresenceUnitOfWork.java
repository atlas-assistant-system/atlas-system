package atlas.infrastructure.presence.persistence;

import atlas.application.presence.ports.AuthenticationGateRepository;
import atlas.application.presence.ports.AuthenticationSessionRepository;
import atlas.application.presence.ports.BiometricProfileRepository;
import atlas.application.presence.ports.PresenceUnitOfWork;
import atlas.application.sharedkernel.events.EventDelivery;
import atlas.application.sharedkernel.unitofwork.AbstractUnitOfWork;
import atlas.infrastructure.sharedkernel.SequenceGenerator;
import atlas.infrastructure.sharedkernel.persistence.PersistenceException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.locks.ReentrantLock;

public final class SqlitePresenceUnitOfWork extends AbstractUnitOfWork implements PresenceUnitOfWork {

    private final Connection connection;
    // ponytail: one SQLite writer is serialized; add a connection pool only if measured throughput requires it.
    private final ReentrantLock transaction = new ReentrantLock();
    private final SqliteBiometricProfileRepository profiles;
    private final SqliteAuthenticationSessionRepository sessions;
    private final SqliteAuthenticationGateRepository gate;

    public SqlitePresenceUnitOfWork(Connection connection, EventDelivery delivery, SequenceGenerator sequences) {
        super(delivery);
        this.connection = connection;
        this.profiles = new SqliteBiometricProfileRepository(connection, sequences);
        this.sessions = new SqliteAuthenticationSessionRepository(connection, sequences);
        this.gate = new SqliteAuthenticationGateRepository(connection);
    }

    @Override
    public BiometricProfileRepository profiles() {
        return profiles;
    }

    @Override
    public AuthenticationSessionRepository sessions() {
        return sessions;
    }

    @Override
    public AuthenticationGateRepository gate() {
        return gate;
    }

    @Override
    protected void begin() {
        transaction.lock();
        try {
            connection.setAutoCommit(false);
        } catch (SQLException e) {
            transaction.unlock();
            throw new PersistenceException("Failed to begin a transaction", e);
        }
    }

    @Override
    protected void commit() {
        try {
            connection.commit();
            transaction.unlock();
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
        } finally {
            transaction.unlock();
        }
    }
}
