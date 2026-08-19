package atlas.infrastructure.sharedkernel.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SchemaMigratorTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-16T10:15:30Z"), ZoneOffset.UTC);

    private static final Migration CREATE_NOTES =
        new Migration(1, "create_notes", "CREATE TABLE notes (id INTEGER PRIMARY KEY, text TEXT NOT NULL)");

    private static final Migration ADD_COLUMN =
        new Migration(2, "add_pinned", "ALTER TABLE notes ADD COLUMN pinned INTEGER NOT NULL DEFAULT 0");

    private Connection connection;
    private SchemaMigrator migrator;

    @BeforeEach
    void setUp() throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        connection.setAutoCommit(false);
        migrator = new SchemaMigrator(connection, CLOCK);
    }

    @AfterEach
    void tearDown() throws SQLException {
        connection.close();
    }

    private boolean columnExists(String table, String column) throws SQLException {
        try (var statement = connection.createStatement();
            var rows = statement.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rows.next()) {
                if (column.equals(rows.getString("name"))) {
                    return true;
                }
            }
        }

        return false;
    }

    @Test
    void shouldApplyEveryMigrationWhenDatabaseIsNew() throws SQLException {
        var applied = migrator.migrate(List.of(CREATE_NOTES, ADD_COLUMN));

        assertThat(applied).isEqualTo(2);
        assertThat(columnExists("notes", "pinned")).isTrue();
    }

    @Test
    void shouldApplyInVersionOrderWhenGivenOutOfOrder() throws SQLException {
        migrator.migrate(List.of(ADD_COLUMN, CREATE_NOTES));

        assertThat(columnExists("notes", "pinned")).isTrue();
    }

    @Test
    void shouldSkipAppliedMigrationsWhenRunAgain() {
        migrator.migrate(List.of(CREATE_NOTES, ADD_COLUMN));

        var applied = migrator.migrate(List.of(CREATE_NOTES, ADD_COLUMN));

        assertThat(applied).isZero();
        assertThat(migrator.appliedVersions()).containsExactly(1, 2);
    }

    @Test
    void shouldApplyOnlyTheNewOneWhenAMigrationIsAdded() {
        migrator.migrate(List.of(CREATE_NOTES));

        var applied = migrator.migrate(List.of(CREATE_NOTES, ADD_COLUMN));

        assertThat(applied).isEqualTo(1);
    }

    @Test
    void shouldThrowWhenAnAppliedMigrationWasModified() {
        migrator.migrate(List.of(CREATE_NOTES));

        var edited = new Migration(1, "create_notes", "CREATE TABLE notes (id INTEGER PRIMARY KEY, body TEXT)");

        assertThatThrownBy(() -> migrator.migrate(List.of(edited)))
            .isInstanceOf(PersistenceException.class)
            .hasMessageContaining("was modified after being applied");
    }

    @Test
    void shouldThrowWhenTwoMigrationsShareAVersion() {
        var duplicate = new Migration(1, "other", "CREATE TABLE other (id INTEGER PRIMARY KEY)");

        assertThatThrownBy(() -> migrator.migrate(List.of(CREATE_NOTES, duplicate)))
            .isInstanceOf(PersistenceException.class)
            .hasMessageContaining("Duplicate migration version 1");
    }

    @Test
    void shouldThrowWhenANewMigrationIsOlderThanTheLastApplied() {
        var third = new Migration(3, "create_tags", "CREATE TABLE tags (id INTEGER PRIMARY KEY)");
        migrator.migrate(List.of(third));

        assertThatThrownBy(() -> migrator.migrate(List.of(CREATE_NOTES, third)))
            .isInstanceOf(PersistenceException.class)
            .hasMessageContaining("older than the last applied version V3");
    }

    @Test
    void shouldAcceptANewMigrationWhenItIsNewerThanEveryApplied() {
        migrator.migrate(List.of(CREATE_NOTES));

        assertThatCode(() -> migrator.migrate(List.of(CREATE_NOTES, ADD_COLUMN))).doesNotThrowAnyException();
    }

    @Test
    void shouldNotRecordVersionWhenSqlFails() {
        var broken = new Migration(1, "broken", "CREATE TABLE (((");

        assertThatThrownBy(() -> migrator.migrate(List.of(broken)))
            .isInstanceOf(PersistenceException.class)
            .hasMessageContaining("V1");

        assertThat(migrator.appliedVersions()).isEmpty();
    }

    @Test
    void shouldStopAtTheFailingMigrationWhenOneOfSeveralFails() {
        var broken = new Migration(2, "broken", "ALTER TABLE missing_table ADD COLUMN x INTEGER");

        assertThatThrownBy(() -> migrator.migrate(List.of(CREATE_NOTES, broken)))
            .isInstanceOf(PersistenceException.class);

        assertThat(migrator.appliedVersions()).containsExactly(1);
    }
}
