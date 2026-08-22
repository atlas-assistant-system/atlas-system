package atlas.application.sharedkernel.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import atlas.application.sharedkernel.events.SimpleDomainEventPublisher;
import atlas.application.sharedkernel.unitofwork.AggregateChanges;
import atlas.infrastructure.sharedkernel.persistence.SqlOutboxStore;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OutboxTest {

    private static final Instant NOW = Instant.parse("2026-08-16T10:15:30Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private final List<String> published = new ArrayList<>();
    private final SimpleDomainEventPublisher publisher = new SimpleDomainEventPublisher();

    private Connection connection;
    private SqlOutboxStore store;
    private OutboxProcessor processor;
    private OutboxUnitOfWork unitOfWork;
    private boolean handlerFails;

    @BeforeEach
    void setUp() throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        store = new SqlOutboxStore(connection);
        store.createTableIfMissing();
        processor = new OutboxProcessor(store, new TextSerializer(NOW), publisher, CLOCK);
        unitOfWork = new OutboxUnitOfWork(connection, store, new TextSerializer(NOW), processor);

        publisher.subscribe(NoteRenamed.class, event -> {
            if (handlerFails) {
                throw new IllegalStateException("projection is down");
            }

            published.add("renamed:" + event.id());
        });
    }

    @AfterEach
    void tearDown() throws SQLException {
        connection.close();
    }

    @Test
    void shouldDeliverEventWhenWorkCommits() {
        var note = new Note(1);

        unitOfWork.run(() -> {
            note.rename(NOW);
            AggregateChanges.track(note);
        });

        assertThat(published).containsExactly("renamed:1");
        assertThat(store.pending(10, 3)).isEmpty();
    }

    @Test
    void shouldKeepEventForRetryWhenHandlerFails() {
        handlerFails = true;
        var note = new Note(1);

        unitOfWork.run(() -> {
            note.rename(NOW);
            AggregateChanges.track(note);
        });

        assertThat(published).isEmpty();
        assertThat(store.pending(10, 3))
            .singleElement()
            .satisfies(message -> {
                assertThat(message.retryCount()).isEqualTo(1);
                assertThat(message.error()).contains("projection is down");
                assertThat(message.isPending()).isTrue();
            });
    }

    @Test
    void shouldDeliverOnRetryWhenHandlerRecovers() {
        handlerFails = true;
        var note = new Note(1);

        unitOfWork.run(() -> {
            note.rename(NOW);
            AggregateChanges.track(note);
        });

        handlerFails = false;
        var delivered = processor.process();

        assertThat(delivered).isEqualTo(1);
        assertThat(published).containsExactly("renamed:1");
        assertThat(store.pending(10, 3)).isEmpty();
    }

    @Test
    void shouldStopRetryingWhenMaxRetryCountIsReached() {
        handlerFails = true;
        var note = new Note(1);

        unitOfWork.run(() -> {
            note.rename(NOW);
            AggregateChanges.track(note);
        });

        processor.process(10, 3);
        processor.process(10, 3);

        assertThat(store.pending(10, 3)).isEmpty();
        assertThat(store.pending(10, 99)).singleElement().satisfies(m -> assertThat(m.retryCount()).isEqualTo(3));
    }

    @Test
    void shouldNotWriteToOutboxWhenWorkThrows() {
        var note = new Note(1);

        assertThatThrownBy(() -> unitOfWork.run(() -> {
            note.rename(NOW);
            AggregateChanges.track(note);
            throw new IllegalStateException("boom");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(store.pending(10, 3)).isEmpty();
        assertThat(published).isEmpty();
    }

    @Test
    void shouldClearAggregateEventsWhenWrittenToOutbox() {
        var note = new Note(1);

        unitOfWork.run(() -> {
            note.rename(NOW);
            AggregateChanges.track(note);
        });

        assertThat(note.pendingEvents()).isEmpty();
    }
}
