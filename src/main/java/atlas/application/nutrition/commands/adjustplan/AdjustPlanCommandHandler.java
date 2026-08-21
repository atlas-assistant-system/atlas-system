package atlas.application.nutrition.commands.adjustplan;

import atlas.application.nutrition.dto.PlanDto;
import atlas.application.nutrition.mappers.NutritionMapper;
import atlas.application.nutrition.ports.NutritionUnitOfWork;
import atlas.application.sharedkernel.cqrs.CommandHandler;
import atlas.domain.nutrition.PlanErrors;
import atlas.domain.nutrition.vos.Macros;
import atlas.domain.nutrition.vos.Weight;
import atlas.domain.sharedkernel.results.Result;
import java.time.Clock;

public final class AdjustPlanCommandHandler implements CommandHandler<AdjustPlanCommand, Result<PlanDto>> {

    private final NutritionUnitOfWork unitOfWork;
    private final Clock clock;

    public AdjustPlanCommandHandler(NutritionUnitOfWork unitOfWork, Clock clock) {
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public Result<PlanDto> handle(AdjustPlanCommand command) {
        var targetResult = Weight.ofKilograms(command.targetWeight());
        if (targetResult.isFailure()) {
            return Result.failure(targetResult.error());
        }

        var macrosResult = Macros.create(command.protein(), command.carbs(), command.fat());
        if (macrosResult.isFailure()) {
            return Result.failure(macrosResult.error());
        }

        var now = clock.instant();

        return unitOfWork.execute(() -> {
            var plans = unitOfWork.plans();
            var active = plans.findActive();
            if (active.isEmpty()) {
                return Result.failure(PlanErrors.NONE_ACTIVE);
            }

            var plan = active.get();
            var adjusted = plan.adjust(macrosResult.value(), targetResult.value(), now);
            if (adjusted.isFailure()) {
                return Result.failure(adjusted.error());
            }

            plans.update(plan);

            return Result.success(NutritionMapper.toDto(plan));
        });
    }
}
