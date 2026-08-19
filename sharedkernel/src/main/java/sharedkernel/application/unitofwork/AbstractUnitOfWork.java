package sharedkernel.application.unitofwork;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import sharedkernel.application.events.EventDelivery;
import sharedkernel.application.events.ImmediateEventDelivery;
import sharedkernel.application.events.PendingEventDispatcher;
import sharedkernel.domain.ddd.AggregateRoot;
import sharedkernel.domain.guards.ObjectGuard;
import sharedkernel.domain.results.Result;

public abstract class AbstractUnitOfWork implements UnitOfWork {

    private final EventDelivery delivery;

    protected AbstractUnitOfWork(PendingEventDispatcher dispatcher) {
        this(new ImmediateEventDelivery(dispatcher));
    }

    protected AbstractUnitOfWork(EventDelivery delivery) {
        this.delivery = ObjectGuard.notNull(delivery, "delivery");
    }

    @Override
    public final <T> T execute(Supplier<T> work) {
        ObjectGuard.notNull(work, "work");

        if (AggregateChanges.isTracking()) {
            throw new IllegalStateException("Nested units of work are not supported.");
        }

        List<AggregateRoot<?>> changed = new ArrayList<>();
        T result;

        begin();

        try {
            result = AggregateChanges.collectInto(changed, work);

            if (result instanceof Result<?> outcome && outcome.isFailure()) {
                rollback();
                return result;
            }

            delivery.beforeCommit(changed);
            commit();
        } catch (RuntimeException e) {
            rollback();
            throw e;
        }

        delivery.afterCommit(changed);

        return result;
    }

    @Override
    public final void run(Runnable work) {
        ObjectGuard.notNull(work, "work");

        execute(() -> {
            work.run();
            return null;
        });
    }

    protected abstract void begin();

    protected abstract void commit();

    protected abstract void rollback();
}
