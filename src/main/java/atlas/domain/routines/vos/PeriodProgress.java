package atlas.domain.routines.vos;

import java.math.BigDecimal;
import java.time.LocalDate;
import sharedkernel.domain.ddd.ValueObject;
import sharedkernel.domain.guards.ObjectGuard;

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
