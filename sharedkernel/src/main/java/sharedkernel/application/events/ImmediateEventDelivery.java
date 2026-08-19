package sharedkernel.application.events;

import java.util.List;
import sharedkernel.domain.ddd.AggregateRoot;
import sharedkernel.domain.guards.ObjectGuard;

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
