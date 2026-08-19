package atlas.infrastructure.sharedkernel.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import atlas.domain.sharedkernel.exceptions.GuardException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SqliteConnectionsTest {

    @TempDir
    Path databaseDirectory;

    @Test
    void shouldOpenOneFilePerBoundedContext() throws SQLException {
        try (var appointments = SqliteConnections.openForContext(databaseDirectory, "appointments");
            var reminders = SqliteConnections.openForContext(databaseDirectory, "reminders")) {
            assertThat(appointments.isClosed()).isFalse();
            assertThat(reminders.isClosed()).isFalse();
        }

        assertThat(databaseDirectory.resolve("appointments.db")).exists();
        assertThat(databaseDirectory.resolve("reminders.db")).exists();
    }

    @Test
    void shouldLeaveTransactionControlToTheCallerWhenOpening() throws SQLException {
        try (var connection = SqliteConnections.openForContext(databaseDirectory, "appointments")) {
            assertThat(connection.getAutoCommit()).isFalse();
        }
    }

    @Test
    void shouldCreateTheDirectoryWhenItDoesNotExistYet() throws SQLException, IOException {
        var nested = databaseDirectory.resolve("data").resolve("sqlite");

        try (var connection = SqliteConnections.openForContext(nested, "appointments")) {
            assertThat(connection.isClosed()).isFalse();
        }

        assertThat(Files.isDirectory(nested)).isTrue();
    }

    @Test
    void shouldRejectAContextNameThatWouldEscapeTheDirectory() {
        assertThatThrownBy(() -> SqliteConnections.openForContext(databaseDirectory, "../../etc/passwd"))
            .isInstanceOf(PersistenceException.class)
            .hasMessageContaining("Not a valid bounded context name");
    }

    @Test
    void shouldRejectABlankContextName() {
        assertThatThrownBy(() -> SqliteConnections.openForContext(databaseDirectory, " "))
            .isInstanceOf(GuardException.class);
    }
}
