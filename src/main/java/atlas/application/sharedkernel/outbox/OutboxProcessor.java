package atlas.application.sharedkernel.outbox;

import atlas.application.sharedkernel.events.DomainEventPublisher;
import atlas.application.sharedkernel.events.DomainEventSerializer;
import atlas.domain.sharedkernel.guards.ObjectGuard;
import java.lang.System.Logger.Level;
import java.time.Clock;
import java.time.Instant;

public final class OutboxProcessor {

    public static final int DEFAULT_BATCH_SIZE = 100;
    public static final int DEFAULT_MAX_RETRY_COUNT = 3;

    private static final System.Logger LOG = System.getLogger("sharedkernel.outbox");
    private static final int MAX_ERROR_LENGTH = 500;

    private final OutboxStore store;
    private final DomainEventSerializer serializer;
    private final DomainEventPublisher publisher;
    private final Clock clock;

    public OutboxProcessor(
        OutboxStore store, DomainEventSerializer serializer, DomainEventPublisher publisher, Clock clock) {
        this.store = ObjectGuard.notNull(store, "store");
        this.serializer = ObjectGuard.notNull(serializer, "serializer");
        this.publisher = ObjectGuard.notNull(publisher, "publisher");
        this.clock = ObjectGuard.notNull(clock, "clock");
    }

    public int process() {
        return process(DEFAULT_BATCH_SIZE, DEFAULT_MAX_RETRY_COUNT);
    }

    public int process(int batchSize, int maxRetryCount) {
        var pending = store.pending(batchSize, maxRetryCount);
        var delivered = 0;

        for (var message : pending) {
            if (deliver(message)) {
                delivered++;
            }
        }

        return delivered;
    }

    private boolean deliver(OutboxMessage message) {
        try {
            publisher.publish(serializer.deserialize(message.type(), message.payload()));
            store.markProcessed(message.id(), Instant.now(clock));

            return true;
        } catch (RuntimeException e) {
            store.markFailed(message.id(), describe(e));
            LOG.log(Level.WARNING, "Outbox delivery failed for " + message.type() + " (" + message.id() + ")", e);

            return false;
        }
    }

    private static String describe(RuntimeException thrown) {
        var description = thrown.getClass().getSimpleName() + ": " + thrown.getMessage();

        return description.length() <= MAX_ERROR_LENGTH
            ? description
            : description.substring(0, MAX_ERROR_LENGTH);
    }
}
