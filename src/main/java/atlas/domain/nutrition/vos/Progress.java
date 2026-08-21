package atlas.domain.nutrition.vos;

import atlas.domain.nutrition.enums.Goal;
import atlas.domain.sharedkernel.ddd.ValueObject;
import atlas.domain.sharedkernel.guards.NumberGuard;
import atlas.domain.sharedkernel.guards.ObjectGuard;

public record Progress(
    Weight start,
    Weight current,
    Weight target,
    Goal goal,
    int remainingGrams,
    int percentage,
    boolean reached,
    int trendGramsPerWeek) implements ValueObject {

    public Progress {
        ObjectGuard.notNull(start, "start");
        ObjectGuard.notNull(current, "current");
        ObjectGuard.notNull(target, "target");
        ObjectGuard.notNull(goal, "goal");
        NumberGuard.inRange(percentage, 0, 100, "percentage");
    }
}
