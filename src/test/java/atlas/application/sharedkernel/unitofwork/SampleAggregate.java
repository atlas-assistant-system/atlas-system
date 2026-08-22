package atlas.application.sharedkernel.unitofwork;

import atlas.domain.sharedkernel.ddd.AggregateRoot;
import java.time.Instant;

final class SampleAggregate extends AggregateRoot<SampleId> {

    private static final Instant NOW = Instant.parse("2026-08-16T10:15:30Z");

    SampleAggregate(long id) {
        super(new SampleId(id));
    }

    void change() {
        registerEvent(new SampleChanged(id(), NOW));
    }
}
