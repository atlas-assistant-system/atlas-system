package atlas.application.nutrition.commands.defineplan;

import atlas.application.nutrition.dto.PlanDto;
import atlas.application.nutrition.mappers.NutritionMapper;
import atlas.application.nutrition.ports.NutritionUnitOfWork;
import atlas.application.sharedkernel.cqrs.CommandHandler;
import atlas.domain.nutrition.Plan;
import atlas.domain.nutrition.vos.Calories;
import atlas.domain.nutrition.vos.Macros;
import atlas.domain.nutrition.vos.Weight;
import atlas.domain.sharedkernel.results.Result;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

public final class DefinePlanCommandHandler implements CommandHandler<DefinePlanCommand, Result<PlanDto>> {

    private final NutritionUnitOfWork unitOfWork;
    private final Clock clock;

    public DefinePlanCommandHandler(NutritionUnitOfWork unitOfWork, Clock clock) {
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public Result<PlanDto> handle(DefinePlanCommand command) {
        var startResult = Weight.ofKilograms(command.startWeight());
        if (startResult.isFailure()) {
            return Result.failure(startResult.error());
        }

        var targetResult = Weight.ofKilograms(command.targetWeight());
        if (targetResult.isFailure()) {
            return Result.failure(targetResult.error());
        }

        var caloriesResult = Calories.create(command.calories());
        if (caloriesResult.isFailure()) {
            return Result.failure(caloriesResult.error());
        }

        var macrosResult = Macros.create(command.protein(), command.carbs(), command.fat());
        if (macrosResult.isFailure()) {
            return Result.failure(macrosResult.error());
        }

        var today = LocalDate.now(clock);
        var startedOn = command.startedOn() == null ? today : command.startedOn();
        var now = clock.instant();

        return unitOfWork.execute(() -> {
            var plans = unitOfWork.plans();

            var archived = archiveActive(now);
            if (archived.isFailure()) {
                return Result.failure(archived.error());
            }

            var defined = Plan.define(
                plans.nextId(), startResult.value(), targetResult.value(), caloriesResult.value(),
                macrosResult.value(), startedOn, today, now);
            if (defined.isFailure()) {
                return Result.failure(defined.error());
            }

            var plan = defined.value();
            plans.create(plan);

            return Result.success(NutritionMapper.toDto(plan));
        });
    }

    private Result<Void> archiveActive(Instant now) {
        var plans = unitOfWork.plans();
        var active = plans.findActive();
        if (active.isEmpty()) {
            return Result.success();
        }

        var plan = active.get();
        var archived = plan.archive(now);
        if (archived.isFailure()) {
            return archived;
        }

        plans.update(plan);

        return Result.success();
    }
}
