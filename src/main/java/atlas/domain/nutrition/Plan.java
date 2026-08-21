package atlas.domain.nutrition;

import atlas.domain.nutrition.enums.Goal;
import atlas.domain.nutrition.enums.PlanStatus;
import atlas.domain.nutrition.events.PlanAdjustedEvent;
import atlas.domain.nutrition.events.PlanArchivedEvent;
import atlas.domain.nutrition.events.PlanDefinedEvent;
import atlas.domain.nutrition.vos.Calories;
import atlas.domain.nutrition.vos.Macros;
import atlas.domain.nutrition.vos.Weight;
import atlas.domain.sharedkernel.ddd.AggregateRoot;
import atlas.domain.sharedkernel.guards.ObjectGuard;
import atlas.domain.sharedkernel.results.Result;
import java.time.Instant;
import java.time.LocalDate;

public final class Plan extends AggregateRoot<PlanId> {

    private final Weight startWeight;
    private final LocalDate startedOn;
    private final Instant definedAt;

    private Weight targetWeight;
    private Macros dailyMacros;
    private PlanStatus status;

    private Plan(
        PlanId id,
        Weight startWeight,
        Weight targetWeight,
        Macros dailyMacros,
        PlanStatus status,
        LocalDate startedOn,
        Instant definedAt) {
        super(ObjectGuard.notNull(id, "id"));
        this.startWeight = ObjectGuard.notNull(startWeight, "startWeight");
        this.targetWeight = ObjectGuard.notNull(targetWeight, "targetWeight");
        this.dailyMacros = ObjectGuard.notNull(dailyMacros, "dailyMacros");
        this.status = ObjectGuard.notNull(status, "status");
        this.startedOn = ObjectGuard.notNull(startedOn, "startedOn");
        this.definedAt = ObjectGuard.notNull(definedAt, "definedAt");
    }

    public static Result<Plan> define(
        PlanId id,
        Weight startWeight,
        Weight targetWeight,
        Macros dailyMacros,
        LocalDate startedOn,
        LocalDate today,
        Instant now) {

        if (dailyMacros.isZero()) {
            return Result.failure(PlanErrors.MACROS_REQUIRED);
        }

        if (startedOn.isAfter(today)) {
            return Result.failure(PlanErrors.CANNOT_START_IN_THE_FUTURE);
        }

        var plan = new Plan(id, startWeight, targetWeight, dailyMacros, PlanStatus.ACTIVE, startedOn, now);
        plan.registerEvent(new PlanDefinedEvent(id, now));

        return Result.success(plan);
    }

    public static Plan rehydrate(
        PlanId id,
        Weight startWeight,
        Weight targetWeight,
        Macros dailyMacros,
        PlanStatus status,
        LocalDate startedOn,
        Instant definedAt) {

        return new Plan(id, startWeight, targetWeight, dailyMacros, status, startedOn, definedAt);
    }

    public Result<Void> adjust(Macros newDailyMacros, Weight newTargetWeight, Instant now) {
        if (isArchived()) {
            return Result.failure(PlanErrors.ALREADY_ARCHIVED);
        }

        if (newDailyMacros.isZero()) {
            return Result.failure(PlanErrors.MACROS_REQUIRED);
        }

        this.dailyMacros = newDailyMacros;
        this.targetWeight = ObjectGuard.notNull(newTargetWeight, "newTargetWeight");
        registerEvent(new PlanAdjustedEvent(id(), now));

        return Result.success();
    }

    public Result<Void> archive(Instant now) {
        if (isArchived()) {
            return Result.failure(PlanErrors.ALREADY_ARCHIVED);
        }

        this.status = PlanStatus.ARCHIVED;
        registerEvent(new PlanArchivedEvent(id(), now));

        return Result.success();
    }

    public Goal goal() {
        return Goal.of(startWeight, targetWeight);
    }

    public Calories dailyCalories() {
        return dailyMacros.calories();
    }

    public boolean isArchived() {
        return status == PlanStatus.ARCHIVED;
    }

    public Weight startWeight() {
        return startWeight;
    }

    public Weight targetWeight() {
        return targetWeight;
    }

    public Macros dailyMacros() {
        return dailyMacros;
    }

    public PlanStatus status() {
        return status;
    }

    public LocalDate startedOn() {
        return startedOn;
    }

    public Instant definedAt() {
        return definedAt;
    }
}
