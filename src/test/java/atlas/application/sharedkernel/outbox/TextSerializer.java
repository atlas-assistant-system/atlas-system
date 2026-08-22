package atlas.application.sharedkernel.outbox;

import atlas.application.sharedkernel.events.DomainEventSerializer;
import atlas.domain.sharedkernel.events.DomainEvent;
import java.time.Instant;

final class TextSerializer implements DomainEventSerializer {

    private final Instant now;

    TextSerializer(Instant now) {
        this.now = now;
    }

    @Override
    public String typeOf(DomainEvent event) {
        return event.getClass().getSimpleName();
    }

    @Override
    public String serialize(DomainEvent event) {
        return String.valueOf(((NoteRenamed) event).id());
    }

    @Override
    public DomainEvent deserialize(String type, String payload) {
        return new NoteRenamed(Long.parseLong(payload), now);
    }
}
