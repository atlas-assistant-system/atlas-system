package atlas.domain.sharedkernel.ddd;

import java.time.Instant;

final class SampleAggregate extends AggregateRoot<SampleId> {

    private SampleAggregate(SampleId id) {
        super(id);
    }

    static SampleAggregate create(SampleId id, Instant now) {
        var aggregate = new SampleAggregate(id);
        aggregate.registerEvent(new SampleCreatedEvent(id, now));

        return aggregate;
    }

    void registerUntimedEvent() {
        registerEvent(new UntimedEvent());
    }
}
