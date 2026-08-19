package atlas.domain.routines.vos;

import sharedkernel.domain.ddd.ValueObject;

public record Streak(int current, int best) implements ValueObject {

    public static final Streak NONE = new Streak(0, 0);
}
