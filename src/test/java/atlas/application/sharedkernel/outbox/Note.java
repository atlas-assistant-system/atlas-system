package atlas.application.sharedkernel.outbox;

import atlas.domain.sharedkernel.ddd.AggregateRoot;
import java.time.Instant;

final class Note extends AggregateRoot<NoteId> {

    Note(long id) {
        super(new NoteId(id));
    }

    void rename(Instant now) {
        registerEvent(new NoteRenamed(id().value(), now));
    }
}
