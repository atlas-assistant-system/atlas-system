package atlas.domain.routines;

import atlas.domain.routines.events.DayClearedEvent;
import atlas.domain.routines.events.ProgressLoggedEvent;
import atlas.domain.routines.vos.Target;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import sharedkernel.domain.ddd.AggregateRoot;
import sharedkernel.domain.guards.ObjectGuard;
import sharedkernel.domain.results.Result;

public final class RoutineEntry extends AggregateRoot<RoutineEntryId> {

    private final RoutineId routineId;
    private final LocalDate day;
    private BigDecimal amount;

    private RoutineEntry(RoutineEntryId id, RoutineId routineId, LocalDate day, BigDecimal amount) {
        super(ObjectGuard.notNull(id, "id"));
        this.routineId = ObjectGuard.notNull(routineId, "routineId");
        this.day = ObjectGuard.notNull(day, "day");
        this.amount = ObjectGuard.notNull(amount, "amount");
    }

    public static Result<RoutineEntry> log(
        RoutineEntryId id,
        RoutineId routineId,
        LocalDate day,
        BigDecimal amount,
        Instant occurredOn) {

        if (amount == null || amount.signum() <= 0) {
            return Result.failure(RoutineErrors.AMOUNT_MUST_BE_POSITIVE);
        }

        var entry = new RoutineEntry(id, routineId, day, Target.normalize(amount));
        entry.registerEvent(new ProgressLoggedEvent(routineId, day, entry.amount, occurredOn));

        return Result.success(entry);
    }

    public static RoutineEntry rehydrate(RoutineEntryId id, RoutineId routineId, LocalDate day, BigDecimal amount) {
        return new RoutineEntry(id, routineId, day, amount);
    }

    public Result<Void> add(BigDecimal extra, Instant occurredOn) {
        if (extra == null || extra.signum() <= 0) {
            return Result.failure(RoutineErrors.AMOUNT_MUST_BE_POSITIVE);
        }

        this.amount = Target.normalize(this.amount.add(extra));
        registerEvent(new ProgressLoggedEvent(routineId, day, extra, occurredOn));

        return Result.success();
    }

    public Result<Void> clear(Instant occurredOn) {
        registerEvent(new DayClearedEvent(routineId, day, occurredOn));

        return Result.success();
    }

    public RoutineId routineId() {
        return routineId;
    }

    public LocalDate day() {
        return day;
    }

    public BigDecimal amount() {
        return amount;
    }
}
