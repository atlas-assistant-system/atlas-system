package atlas.infrastructure.sharedkernel.persistence;

import atlas.domain.sharedkernel.ddd.AggregateRoot;
import java.time.Instant;

final class Note extends AggregateRoot<NoteId> {

    private String text;

    Note(long id, String text) {
        super(new NoteId(id));
        this.text = text;
    }

    void rename(String newText, Instant now) {
        this.text = newText;
        registerEvent(new NoteRenamed(id(), now));
    }

    String text() {
        return text;
    }
}
