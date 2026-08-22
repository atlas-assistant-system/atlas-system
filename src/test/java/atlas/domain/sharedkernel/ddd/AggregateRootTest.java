package atlas.domain.sharedkernel.ddd;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import atlas.domain.sharedkernel.events.DomainEvent;
import atlas.domain.sharedkernel.exceptions.GuardException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class AggregateRootTest {

    private static final Instant NOW = Instant.parse("2026-08-16T10:15:30Z");

    @Test
    void shouldAccumulateEventsWhenBehaviorRegistersThem() {
        var aggregate = SampleAggregate.create(new SampleId(1), NOW);

        assertThat(aggregate.pendingEvents())
            .containsExactly(new SampleCreatedEvent(new SampleId(1), NOW));
    }

    @Test
    void shouldStampEventWithProvidedInstantWhenRegistered() {
        var aggregate = SampleAggregate.create(new SampleId(1), NOW);

        assertThat(aggregate.pendingEvents())
            .singleElement()
            .extracting(DomainEvent::occurredOn)
            .isEqualTo(NOW);
    }

    @Test
    void shouldThrowWhenEventHasNoTimestamp() {
        var aggregate = SampleAggregate.create(new SampleId(1), NOW);

        assertThatThrownBy(aggregate::registerUntimedEvent)
            .isInstanceOf(GuardException.class);
    }

    @Test
    void shouldHaveNoEventsWhenCleared() {
        var aggregate = SampleAggregate.create(new SampleId(1), NOW);

        aggregate.clearEvents();

        assertThat(aggregate.pendingEvents()).isEmpty();
    }

    @Test
    void shouldRejectExternalMutationWhenExposingPendingEvents() {
        var aggregate = SampleAggregate.create(new SampleId(1), NOW);

        assertThatThrownBy(() -> aggregate.pendingEvents().clear())
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldBeEqualWhenIdsMatch() {
        var first = SampleAggregate.create(new SampleId(7), NOW);
        var second = SampleAggregate.create(new SampleId(7), NOW);
        var different = SampleAggregate.create(new SampleId(8), NOW);

        assertThat(first).isEqualTo(second);
        assertThat(first).hasSameHashCodeAs(second);
        assertThat(first).isNotEqualTo(different);
    }

    @Test
    void shouldThrowWhenConstructedWithNullId() {
        assertThatThrownBy(() -> SampleAggregate.create(null, NOW))
            .isInstanceOf(GuardException.class);
    }
}
