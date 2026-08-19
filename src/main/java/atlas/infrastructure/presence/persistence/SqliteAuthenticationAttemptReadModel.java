package atlas.infrastructure.presence.persistence;

import atlas.application.presence.ports.AuthenticationAttempt;
import atlas.application.presence.ports.AuthenticationAttemptReadModel;
import atlas.application.sharedkernel.paging.Page;
import atlas.application.sharedkernel.paging.PageRequest;
import atlas.domain.presence.enums.VerificationOutcome;
import atlas.infrastructure.sharedkernel.persistence.PersistenceException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;

public final class SqliteAuthenticationAttemptReadModel implements AuthenticationAttemptReadModel {

    private final Connection connection;

    public SqliteAuthenticationAttemptReadModel(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Page<AuthenticationAttempt> find(PageRequest page) {
        var sql = "SELECT occurred_on, outcome FROM authentication_attempts"
            + " ORDER BY occurred_on DESC, id DESC LIMIT ? OFFSET ?";

        try (var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, page.pageSize());
            statement.setInt(2, page.offset());

            var attempts = new ArrayList<AuthenticationAttempt>();
            try (var rows = statement.executeQuery()) {
                while (rows.next()) {
                    attempts.add(new AuthenticationAttempt(
                        Instant.parse(rows.getString("occurred_on")),
                        VerificationOutcome.valueOf(rows.getString("outcome"))));
                }
            }

            return Page.of(attempts, page, count());
        } catch (SQLException e) {
            throw new PersistenceException("Failed to load authentication attempts", e);
        }
    }

    private long count() throws SQLException {
        try (var statement = connection.createStatement();
            var rows = statement.executeQuery("SELECT COUNT(*) FROM authentication_attempts")) {
            return rows.next() ? rows.getLong(1) : 0;
        }
    }
}
