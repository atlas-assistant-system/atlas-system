package atlas.application.sharedkernel.logging;

import java.util.Optional;
import java.util.function.Supplier;

public final class CorrelationContext {

    private static final ScopedValue<String> CURRENT = ScopedValue.newInstance();

    private CorrelationContext() {}

    public static void runWith(String correlationId, Runnable action) {
        ScopedValue.where(CURRENT, correlationId).run(action);
    }

    public static <T> T callWith(String correlationId, Supplier<T> action) {
        return ScopedValue.where(CURRENT, correlationId).call(action::get);
    }

    public static Optional<String> current() {
        if (!CURRENT.isBound()) {
            return Optional.empty();
        }

        return Optional.of(CURRENT.get());
    }
}
