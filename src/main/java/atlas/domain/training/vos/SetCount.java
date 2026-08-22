package atlas.domain.training.vos;

import atlas.domain.sharedkernel.ddd.ValueObject;
import atlas.domain.sharedkernel.guards.NumberGuard;
import atlas.domain.sharedkernel.results.Result;
import atlas.domain.training.WorkoutErrors;

/**
 * Cuántas series lleva una línea de la plantilla. El rango no es fisiológico: existe para
 * que un {@code 4x8} tecleado como {@code 48x8} no llegue a la tabla.
 */
public record SetCount(int value) implements ValueObject {

    public static final int MINIMUM = 1;
    public static final int MAXIMUM = 20;

    public SetCount {
        NumberGuard.inRange(value, MINIMUM, MAXIMUM, "value");
    }

    public static Result<SetCount> create(int value) {
        if (value < MINIMUM || value > MAXIMUM) {
            return Result.failure(WorkoutErrors.SET_COUNT_OUT_OF_RANGE);
        }

        return Result.success(new SetCount(value));
    }
}
