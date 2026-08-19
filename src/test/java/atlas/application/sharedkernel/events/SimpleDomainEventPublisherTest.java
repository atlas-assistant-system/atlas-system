package atlas.application.sharedkernel.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import atlas.domain.sharedkernel.events.DomainEvent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SimpleDomainEventPublisherTest {

    private static final Instant NOW = Instant.parse("2026-08-16T10:15:30Z");

    private record SomethingHappened(String detail, Instant occurredOn) implements DomainEvent {}

    private record SomethingElseHappened(Instant occurredOn) implements DomainEvent {}

    private final SimpleDomainEventPublisher publisher = new SimpleDomainEventPublisher();

    @Test
    void shouldNotifyAllSubscribersWhenEventTypeMatches() {
        List<String> received = new ArrayList<>();
        publisher.subscribe(SomethingHappened.class, event -> received.add("first:" + event.detail()));
        publisher.subscribe(SomethingHappened.class, event -> received.add("second:" + event.detail()));

        publisher.publish(new SomethingHappened("x", NOW));

        assertThat(received).containsExactly("first:x", "second:x");
    }

    @Test
    void shouldNotNotifySubscribersWhenEventTypeDiffers() {
        List<String> received = new ArrayList<>();
        publisher.subscribe(SomethingHappened.class, event -> received.add(event.detail()));

        publisher.publish(new SomethingElseHappened(NOW));

        assertThat(received).isEmpty();
    }

    @Test
    void shouldDoNothingWhenNoSubscribersExist() {
        assertThatCode(() -> publisher.publish(new SomethingHappened("x", NOW)))
            .doesNotThrowAnyException();
    }
}
