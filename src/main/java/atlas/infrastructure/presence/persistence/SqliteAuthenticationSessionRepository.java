package atlas.infrastructure.presence.persistence;

import atlas.application.presence.ports.AuthenticationSessionRepository;
import atlas.domain.presence.AuthenticationSession;
import atlas.domain.presence.BiometricProfileId;
import atlas.domain.presence.SessionId;
import atlas.domain.presence.enums.SessionStatus;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import sharedkernel.infrastructure.SequenceGenerator;
import sharedkernel.infrastructure.persistence.AbstractSqlRepository;
import sharedkernel.infrastructure.persistence.PersistenceException;

public final class SqliteAuthenticationSessionRepository
    extends AbstractSqlRepository<AuthenticationSession, SessionId>
    implements AuthenticationSessionRepository {

    private static final String SEQUENCE_NAME = "sessions";

    private final SequenceGenerator sequences;

    public SqliteAuthenticationSessionRepository(Connection connection, SequenceGenerator sequences) {
        super(connection, "sessions", "id");
        this.sequences = sequences;
    }

    @Override
    public SessionId nextId() {
        return SessionId.of(sequences.next(SEQUENCE_NAME));
    }

    @Override
    public List<AuthenticationSession> findActive() {
        return queryActive("SELECT * FROM sessions WHERE status = ? ORDER BY id", Optional.empty());
    }

    @Override
    public Optional<AuthenticationSession> findActiveByProfile(BiometricProfileId profileId) {
        var found = queryActive(
            "SELECT * FROM sessions WHERE status = ? AND profile_id = ? ORDER BY id DESC LIMIT 1",
            Optional.of(profileId));
        return found.stream().findFirst();
    }

    @Override
    protected List<String> columns() {
        return List.of("id", "profile_id", "opened_at", "last_activity_at", "expires_at", "status");
    }

    @Override
    protected void bind(PreparedStatement statement, AuthenticationSession session) throws SQLException {
        statement.setLong(1, session.id().value());
        statement.setLong(2, session.profileId().value());
        statement.setString(3, session.openedAt().toString());
        statement.setString(4, session.lastActivityAt().toString());
        statement.setString(5, session.expiresAt().toString());
        statement.setString(6, session.status().name());
    }

    @Override
    protected AuthenticationSession mapRow(ResultSet row) throws SQLException {
        return AuthenticationSession.rehydrate(
            SessionId.of(row.getLong("id")),
            BiometricProfileId.of(row.getLong("profile_id")),
            Instant.parse(row.getString("opened_at")),
            Instant.parse(row.getString("last_activity_at")),
            Instant.parse(row.getString("expires_at")),
            SessionStatus.valueOf(row.getString("status")));
    }

    @Override
    protected Object idValue(SessionId id) {
        return id.value();
    }

    private List<AuthenticationSession> queryActive(String sql, Optional<BiometricProfileId> profileId) {
        try (var statement = connection().prepareStatement(sql)) {
            statement.setString(1, SessionStatus.ACTIVE.name());
            if (profileId.isPresent()) {
                statement.setLong(2, profileId.get().value());
            }

            try (var rows = statement.executeQuery()) {
                var sessions = new ArrayList<AuthenticationSession>();
                while (rows.next()) {
                    sessions.add(mapRow(rows));
                }
                return List.copyOf(sessions);
            }
        } catch (SQLException e) {
            throw new PersistenceException("Failed to load active sessions", e);
        }
    }
}
