package sharedkernel.application.unitofwork;

import java.util.List;
import java.util.function.Supplier;
import sharedkernel.domain.ddd.AggregateRoot;
import sharedkernel.domain.guards.ObjectGuard;

public final class AggregateChanges {

    private static final ScopedValue<List<AggregateRoot<?>>> CURRENT = ScopedValue.newInstance();

    private AggregateChanges() {}

    public static void track(AggregateRoot<?> aggregate) {
        ObjectGuard.notNull(aggregate, "aggregate");

        if (!CURRENT.isBound()) {
            throw new IllegalStateException(
                "Aggregates can only be tracked inside a unit of work: " + aggregate.getClass().getName());
        }

        var tracked = CURRENT.get();
        for (var existing : tracked) {
            if (existing == aggregate) {
                return;
            }
        }

        tracked.add(aggregate);
    }

    public static boolean isTracking() {
        return CURRENT.isBound();
    }

    static <T> T collectInto(List<AggregateRoot<?>> sink, Supplier<T> work) {
        return ScopedValue.where(CURRENT, sink).call(work::get);
    }
}
