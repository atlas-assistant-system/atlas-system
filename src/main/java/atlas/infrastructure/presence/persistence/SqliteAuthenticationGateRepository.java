package atlas.infrastructure.presence.persistence;

import atlas.application.presence.ports.AuthenticationGateRepository;
import atlas.domain.presence.AuthenticationGate;
import java.sql.Connection;
import java.sql.SQLException;
import sharedkernel.application.unitofwork.AggregateChanges;
import sharedkernel.infrastructure.persistence.PersistenceException;

public final class SqliteAuthenticationGateRepository implements AuthenticationGateRepository {

    private static final long SINGLETON_ID = 1;

    private final Connection connection;

    public SqliteAuthenticationGateRepository(Connection connection) {
        this.connection = connection;
    }

    @Override
    public AuthenticationGate get() {
        var sql = "SELECT failed_attempts FROM authentication_gate WHERE id = ?";

        try (var statement = connection.prepareStatement(sql)) {
            statement.setLong(1, SINGLETON_ID);
            try (var rows = statement.executeQuery()) {
                if (!rows.next()) {
                    return AuthenticationGate.initial();
                }

                return AuthenticationGate.rehydrate(rows.getInt("failed_attempts"));
            }
        } catch (SQLException e) {
            throw new PersistenceException("Failed to load the authentication gate", e);
        }
    }

    @Override
    public void save(AuthenticationGate gate) {
        var sql = "INSERT INTO authentication_gate (id, failed_attempts, locked_until) VALUES (?, ?, ?)"
            + " ON CONFLICT(id) DO UPDATE SET failed_attempts = excluded.failed_attempts,"
            + " locked_until = excluded.locked_until";

        try (var statement = connection.prepareStatement(sql)) {
            statement.setLong(1, SINGLETON_ID);
            statement.setInt(2, gate.failedAttempts());
            statement.setString(3, null);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException("Failed to persist the authentication gate", e);
        }

        AggregateChanges.track(gate);
    }
}
