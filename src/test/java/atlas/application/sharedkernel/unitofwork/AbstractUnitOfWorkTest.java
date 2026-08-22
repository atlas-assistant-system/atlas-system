package atlas.application.sharedkernel.unitofwork;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import atlas.application.sharedkernel.events.SimpleDomainEventPublisher;
import atlas.domain.sharedkernel.results.Error;
import atlas.domain.sharedkernel.results.Result;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class AbstractUnitOfWorkTest {

    private final List<String> journal = new ArrayList<>();
    private final SimpleDomainEventPublisher publisher = new SimpleDomainEventPublisher();

    private RecordingUnitOfWork unitOfWork() {
        return new RecordingUnitOfWork(journal, publisher, false);
    }

    @Test
    void shouldPublishEventsAfterCommitWhenWorkSucceeds() {
        publisher.subscribe(SampleChanged.class, event -> journal.add("published"));
        var aggregate = new SampleAggregate(1);

        unitOfWork().run(() -> {
            aggregate.change();
            AggregateChanges.track(aggregate);
            journal.add("saved");
        });

        assertThat(journal).containsExactly("begin", "saved", "commit", "published");
    }

    @Test
    void shouldReturnWorkResultWhenWorkSucceeds() {
        var result = unitOfWork().execute(() -> "done");

        assertThat(result).isEqualTo("done");
    }

    @Test
    void shouldClearPendingEventsWhenDispatched() {
        var aggregate = new SampleAggregate(1);

        unitOfWork().run(() -> {
            aggregate.change();
            AggregateChanges.track(aggregate);
        });

        assertThat(aggregate.pendingEvents()).isEmpty();
    }

    @Test
    void shouldRollBackAndNotPublishWhenWorkThrows() {
        publisher.subscribe(SampleChanged.class, event -> journal.add("published"));
        var aggregate = new SampleAggregate(1);
        var boom = new IllegalStateException("invalid state");

        assertThatThrownBy(() -> unitOfWork().run(() -> {
            aggregate.change();
            AggregateChanges.track(aggregate);
            throw boom;
        })).isSameAs(boom);

        assertThat(journal).containsExactly("begin", "rollback");
    }

    @Test
    void shouldNotPublishWhenCommitFails() {
        publisher.subscribe(SampleChanged.class, event -> journal.add("published"));
        var aggregate = new SampleAggregate(1);

        assertThatThrownBy(() -> new RecordingUnitOfWork(journal, publisher, true).run(() -> {
            aggregate.change();
            AggregateChanges.track(aggregate);
        })).isInstanceOf(IllegalStateException.class);

        assertThat(journal).containsExactly("begin", "rollback");
        assertThat(aggregate.pendingEvents()).hasSize(1);
    }

    @Test
    void shouldPublishOnceWhenSameAggregateIsTrackedTwice() {
        publisher.subscribe(SampleChanged.class, event -> journal.add("published"));
        var aggregate = new SampleAggregate(1);

        unitOfWork().run(() -> {
            aggregate.change();
            AggregateChanges.track(aggregate);
            AggregateChanges.track(aggregate);
        });

        assertThat(journal).filteredOn("published"::equals).hasSize(1);
    }

    @Test
    void shouldPublishEveryEventWhenAggregateRegistersSeveral() {
        publisher.subscribe(SampleChanged.class, event -> journal.add("published"));
        var aggregate = new SampleAggregate(1);

        unitOfWork().run(() -> {
            aggregate.change();
            aggregate.change();
            AggregateChanges.track(aggregate);
        });

        assertThat(journal).filteredOn("published"::equals).hasSize(2);
    }

    @Test
    void shouldRollBackAndNotPublishWhenWorkReturnsFailedResult() {
        publisher.subscribe(SampleChanged.class, event -> journal.add("published"));
        var aggregate = new SampleAggregate(1);

        var result = unitOfWork().execute(() -> {
            aggregate.change();
            AggregateChanges.track(aggregate);

            return Result.<String>failure(Error.conflict("APPOINTMENT_CANCELLED", "cancelled"));
        });

        assertThat(result.isFailure()).isTrue();
        assertThat(journal).containsExactly("begin", "rollback");
        assertThat(aggregate.pendingEvents()).hasSize(1);
    }

    @Test
    void shouldCommitAndPublishWhenWorkReturnsSuccessfulResult() {
        publisher.subscribe(SampleChanged.class, event -> journal.add("published"));
        var aggregate = new SampleAggregate(1);

        unitOfWork().execute(() -> {
            aggregate.change();
            AggregateChanges.track(aggregate);

            return Result.success("ok");
        });

        assertThat(journal).containsExactly("begin", "commit", "published");
    }

    @Test
    void shouldThrowWhenTrackingOutsideUnitOfWork() {
        var aggregate = new SampleAggregate(1);

        assertThatThrownBy(() -> AggregateChanges.track(aggregate))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("unit of work");
    }

    @Test
    void shouldThrowWhenUnitOfWorkIsNested() {
        var outer = unitOfWork();

        assertThatThrownBy(() -> outer.run(() -> unitOfWork().run(() -> {})))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Nested");
    }

    @Test
    void shouldNotTrackWhenScopeHasEnded() {
        unitOfWork().run(() -> {});

        assertThat(AggregateChanges.isTracking()).isFalse();
    }
}
