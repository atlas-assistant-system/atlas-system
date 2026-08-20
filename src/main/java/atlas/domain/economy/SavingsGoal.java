package atlas.domain.economy;

import atlas.domain.economy.events.SavingsGoalAbandonedEvent;
import atlas.domain.economy.events.SavingsGoalChangedEvent;
import atlas.domain.economy.events.SavingsGoalSetEvent;
import atlas.domain.economy.vos.GoalName;
import atlas.domain.economy.vos.Money;
import atlas.domain.sharedkernel.ddd.AggregateRoot;
import atlas.domain.sharedkernel.guards.ObjectGuard;
import atlas.domain.sharedkernel.results.Result;
import java.time.Instant;
import java.time.LocalDate;

public final class SavingsGoal extends AggregateRoot<SavingsGoalId> {

    private GoalName name;
    private Money target;
    private LocalDate deadline;

    private SavingsGoal(SavingsGoalId id, GoalName name, Money target, LocalDate deadline) {
        super(ObjectGuard.notNull(id, "id"));
        this.name = ObjectGuard.notNull(name, "name");
        this.target = ObjectGuard.notNull(target, "target");
        this.deadline = ObjectGuard.notNull(deadline, "deadline");
    }

    public static Result<SavingsGoal> set(
        SavingsGoalId id, GoalName name, Money target, LocalDate deadline, LocalDate today, Instant now) {

        if (!deadline.isAfter(today)) {
            return Result.failure(SavingsGoalErrors.DEADLINE_MUST_BE_AHEAD);
        }

        var goal = new SavingsGoal(id, name, target, deadline);
        goal.registerEvent(new SavingsGoalSetEvent(id, now));

        return Result.success(goal);
    }

    public static SavingsGoal rehydrate(SavingsGoalId id, GoalName name, Money target, LocalDate deadline) {
        return new SavingsGoal(id, name, target, deadline);
    }

    public Result<Void> change(
        GoalName newName, Money newTarget, LocalDate newDeadline, LocalDate today, Instant now) {

        if (!newDeadline.isAfter(today)) {
            return Result.failure(SavingsGoalErrors.DEADLINE_MUST_BE_AHEAD);
        }

        this.name = ObjectGuard.notNull(newName, "newName");
        this.target = ObjectGuard.notNull(newTarget, "newTarget");
        this.deadline = newDeadline;
        registerEvent(new SavingsGoalChangedEvent(id(), now));

        return Result.success();
    }

    public Result<Void> abandon(Instant now) {
        registerEvent(new SavingsGoalAbandonedEvent(id(), now));

        return Result.success();
    }

    public GoalName name() {
        return name;
    }

    public Money target() {
        return target;
    }

    public LocalDate deadline() {
        return deadline;
    }
}
