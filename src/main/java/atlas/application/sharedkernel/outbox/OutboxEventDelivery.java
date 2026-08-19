package atlas.application.sharedkernel.outbox;

import atlas.application.sharedkernel.events.DomainEventSerializer;
import atlas.application.sharedkernel.events.EventDelivery;
import atlas.domain.sharedkernel.ddd.AggregateRoot;
import atlas.domain.sharedkernel.guards.ObjectGuard;
import java.util.ArrayList;
import java.util.List;

public final class OutboxEventDelivery implements EventDelivery {

    private final OutboxStore store;
    private final DomainEventSerializer serializer;
    private final OutboxProcessor processor;

    public OutboxEventDelivery(OutboxStore store, DomainEventSerializer serializer, OutboxProcessor processor) {
        this.store = ObjectGuard.notNull(store, "store");
        this.serializer = ObjectGuard.notNull(serializer, "serializer");
        this.processor = ObjectGuard.notNull(processor, "processor");
    }

    @Override
    public void beforeCommit(List<? extends AggregateRoot<?>> changed) {
        var messages = new ArrayList<OutboxMessage>();

        for (var aggregate : changed) {
            for (var event : List.copyOf(aggregate.pendingEvents())) {
                messages.add(
                    OutboxMessage.pending(serializer.typeOf(event), serializer.serialize(event), event.occurredOn()));
            }

            aggregate.clearEvents();
        }

        if (!messages.isEmpty()) {
            store.append(messages);
        }
    }

    @Override
    public void afterCommit(List<? extends AggregateRoot<?>> changed) {
        processor.process();
    }
}
