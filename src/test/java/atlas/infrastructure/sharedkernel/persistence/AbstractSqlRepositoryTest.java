package atlas.infrastructure.sharedkernel.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import atlas.application.sharedkernel.events.SimpleDomainEventPublisher;
import atlas.application.sharedkernel.unitofwork.AggregateChanges;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AbstractSqlRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-08-16T10:15:30Z");

    private final SimpleDomainEventPublisher publisher = new SimpleDomainEventPublisher();
    private final List<String> published = new ArrayList<>();

    private Connection connection;
    private NoteRepository notes;
    private UnitOfWorkForTest unitOfWork;

    @BeforeEach
    void setUp() throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        try (var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE notes (id INTEGER PRIMARY KEY, text TEXT NOT NULL)");
        }

        notes = new NoteRepository(connection);
        unitOfWork = new UnitOfWorkForTest(connection, publisher);
        publisher.subscribe(NoteRenamed.class, event -> published.add("renamed:" + event.id().value()));
    }

    @AfterEach
    void tearDown() throws SQLException {
        connection.close();
    }

    @Test
    void shouldBeEmptyWhenIdIsUnknown() {
        assertThat(notes.get(new NoteId(99))).isEmpty();
    }

    @Test
    void shouldReadBackWhenCreated() {
        unitOfWork.run(() -> notes.create(new Note(1, "buy milk")));

        assertThat(notes.get(new NoteId(1))).get().extracting(Note::text).isEqualTo("buy milk");
    }

    @Test
    void shouldReturnEveryRowWhenGettingAll() {
        unitOfWork.run(() -> {
            notes.create(new Note(1, "first"));
            notes.create(new Note(2, "second"));
        });

        assertThat(notes.getAll()).extracting(Note::text).containsExactlyInAnyOrder("first", "second");
    }

    @Test
    void shouldPersistNewValueWhenUpdated() {
        unitOfWork.run(() -> notes.create(new Note(1, "draft")));

        unitOfWork.run(() -> {
            var note = notes.get(new NoteId(1)).orElseThrow();
            note.rename("final", NOW);
            notes.update(note);
        });

        assertThat(notes.get(new NoteId(1))).get().extracting(Note::text).isEqualTo("final");
    }

    @Test
    void shouldPublishEventsWithoutManualTrackingWhenUpdated() {
        unitOfWork.run(() -> notes.create(new Note(1, "draft")));

        unitOfWork.run(() -> {
            var note = notes.get(new NoteId(1)).orElseThrow();
            note.rename("final", NOW);
            notes.update(note);
        });

        assertThat(published).containsExactly("renamed:1");
    }

    @Test
    void shouldRemoveRowWhenDeleted() {
        unitOfWork.run(() -> notes.create(new Note(1, "temporary")));

        unitOfWork.run(() -> notes.delete(notes.get(new NoteId(1)).orElseThrow()));

        assertThat(notes.getAll()).isEmpty();
    }

    @Test
    void shouldThrowWhenUpdatingMissingRow() {
        assertThatThrownBy(() -> unitOfWork.run(() -> notes.update(new Note(42, "ghost"))))
            .isInstanceOf(PersistenceException.class)
            .hasMessageContaining("No row affected");
    }

    @Test
    void shouldThrowWhenWritingOutsideUnitOfWork() {
        assertThatThrownBy(() -> notes.create(new Note(1, "orphan")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("unit of work");
    }

    @Test
    void shouldRejectUnsafeIdentifierWhenTableNameIsNotAnIdentifier() {
        assertThatThrownBy(() -> new AbstractSqlRepository<Note, NoteId>(connection, "notes; DROP TABLE notes", "id") {

            @Override
            protected List<String> columns() {
                return List.of("id");
            }

            @Override
            protected void bind(PreparedStatement statement, Note note) {}

            @Override
            protected Note mapRow(ResultSet row) {
                return null;
            }

            @Override
            protected Object idValue(NoteId id) {
                return id.value();
            }
        }).isInstanceOf(PersistenceException.class).hasMessageContaining("Unsafe SQL identifier");
    }

    @Test
    void shouldNotPersistWhenWorkThrows() {
        assertThatThrownBy(() -> unitOfWork.run(() -> {
            notes.create(new Note(1, "rolled back"));
            throw new IllegalStateException("boom");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(notes.getAll()).isEmpty();
        assertThat(AggregateChanges.isTracking()).isFalse();
    }
}
