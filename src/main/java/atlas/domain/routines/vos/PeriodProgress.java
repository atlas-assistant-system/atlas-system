package atlas.domain.routines.vos;

import atlas.domain.sharedkernel.ddd.ValueObject;
import atlas.domain.sharedkernel.guards.ObjectGuard;
import java.math.BigDecimal;
import java.time.LocalDate;

public record PeriodProgress(PeriodWindow window, BigDecimal logged, Target target) implements ValueObject {

    public PeriodProgress {
        ObjectGuard.notNull(window, "window");
        ObjectGuard.notNull(logged, "logged");
        ObjectGuard.notNull(target, "target");
    }

    public boolean isMet() {
        return target.isMetBy(logged);
    }

    public boolean isClosedOn(LocalDate now) {
        return window.isClosedOn(now);
    }

    public boolean isFailedOn(LocalDate now) {
        return isClosedOn(now) && !isMet();
    }
}
