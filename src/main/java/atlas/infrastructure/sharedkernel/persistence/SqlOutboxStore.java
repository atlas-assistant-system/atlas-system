package atlas.infrastructure.sharedkernel.persistence;

import atlas.application.sharedkernel.outbox.OutboxMessage;
import atlas.application.sharedkernel.outbox.OutboxStore;
import atlas.domain.sharedkernel.guards.ObjectGuard;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class SqlOutboxStore implements OutboxStore {

    public static final String TABLE_NAME = "outbox_messages";

    private static final String CREATE_TABLE = """
        CREATE TABLE IF NOT EXISTS outbox_messages (
            id TEXT PRIMARY KEY,
            type TEXT NOT NULL,
            payload TEXT NOT NULL,
            occurred_on TEXT NOT NULL,
            processed_at TEXT,
            error TEXT,
            retry_count INTEGER NOT NULL DEFAULT 0
        )""";

    private static final String INSERT = """
        INSERT INTO outbox_messages (id, type, payload, occurred_on, processed_at, error, retry_count)
        VALUES (?, ?, ?, ?, NULL, NULL, 0)""";

    private static final String SELECT_PENDING = """
        SELECT * FROM outbox_messages
        WHERE processed_at IS NULL AND retry_count < ?
        ORDER BY occurred_on
        LIMIT ?""";

    private static final String MARK_PROCESSED = "UPDATE outbox_messages SET processed_at = ? WHERE id = ?";

    private static final String MARK_FAILED =
        "UPDATE outbox_messages SET error = ?, retry_count = retry_count + 1 WHERE id = ?";

    private final Connection connection;

    public SqlOutboxStore(Connection connection) {
        this.connection = ObjectGuard.notNull(connection, "connection");
    }

    public void createTableIfMissing() {
        try (var statement = connection.createStatement()) {
            statement.execute(CREATE_TABLE);
        } catch (SQLException e) {
            throw new PersistenceException("Failed to create " + TABLE_NAME, e);
        }
    }

    @Override
    public void append(List<OutboxMessage> messages) {
        ObjectGuard.notNull(messages, "messages");

        try (var statement = connection.prepareStatement(INSERT)) {
            for (var message : messages) {
                statement.setString(1, message.id());
                statement.setString(2, message.type());
                statement.setString(3, message.payload());
                statement.setString(4, message.occurredOn().toString());
                statement.addBatch();
            }

            statement.executeBatch();
        } catch (SQLException e) {
            throw new PersistenceException("Failed to append to " + TABLE_NAME, e);
        }
    }

    @Override
    public List<OutboxMessage> pending(int batchSize, int maxRetryCount) {
        var found = new ArrayList<OutboxMessage>();

        try (var statement = connection.prepareStatement(SELECT_PENDING)) {
            statement.setInt(1, maxRetryCount);
            statement.setInt(2, batchSize);

            try (var rows = statement.executeQuery()) {
                while (rows.next()) {
                    found.add(mapRow(rows));
                }
            }

            return List.copyOf(found);
        } catch (SQLException e) {
            throw new PersistenceException("Failed to read from " + TABLE_NAME, e);
        }
    }

    @Override
    public void markProcessed(String id, Instant processedAt) {
        update(MARK_PROCESSED, processedAt.toString(), id, "mark processed");
    }

    @Override
    public void markFailed(String id, String error) {
        update(MARK_FAILED, error, id, "mark failed");
    }

    private void update(String sql, String firstParameter, String id, String operation) {
        try (var statement = connection.prepareStatement(sql)) {
            statement.setString(1, firstParameter);
            statement.setString(2, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException("Failed to " + operation + " in " + TABLE_NAME, e);
        }
    }

    private static OutboxMessage mapRow(ResultSet row) throws SQLException {
        var processedAt = row.getString("processed_at");

        return new OutboxMessage(
            row.getString("id"),
            row.getString("type"),
            row.getString("payload"),
            Instant.parse(row.getString("occurred_on")),
            processedAt == null ? null : Instant.parse(processedAt),
            row.getString("error"),
            row.getInt("retry_count"));
    }
}
