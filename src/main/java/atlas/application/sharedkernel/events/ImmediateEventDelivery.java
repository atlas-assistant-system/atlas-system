package atlas.application.sharedkernel.events;

import atlas.domain.sharedkernel.ddd.AggregateRoot;
import atlas.domain.sharedkernel.guards.ObjectGuard;
import java.util.List;

public final class ImmediateEventDelivery implements EventDelivery {

    private final PendingEventDispatcher dispatcher;

    public ImmediateEventDelivery(PendingEventDispatcher dispatcher) {
        this.dispatcher = ObjectGuard.notNull(dispatcher, "dispatcher");
    }

    @Override
    public void beforeCommit(List<? extends AggregateRoot<?>> changed) {}

    @Override
    public void afterCommit(List<? extends AggregateRoot<?>> changed) {
        dispatcher.dispatch(changed);
    }
}
