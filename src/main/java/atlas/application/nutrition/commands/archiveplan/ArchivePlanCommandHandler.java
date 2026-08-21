package atlas.application.nutrition.commands.archiveplan;

import atlas.application.nutrition.ports.NutritionUnitOfWork;
import atlas.application.sharedkernel.cqrs.CommandHandler;
import atlas.domain.nutrition.PlanErrors;
import atlas.domain.sharedkernel.results.Result;
import java.time.Clock;

public final class ArchivePlanCommandHandler implements CommandHandler<ArchivePlanCommand, Result<Void>> {

    private final NutritionUnitOfWork unitOfWork;
    private final Clock clock;

    public ArchivePlanCommandHandler(NutritionUnitOfWork unitOfWork, Clock clock) {
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public Result<Void> handle(ArchivePlanCommand command) {
        var now = clock.instant();

        return unitOfWork.execute(() -> {
            var plans = unitOfWork.plans();
            var active = plans.findActive();
            if (active.isEmpty()) {
                return Result.failure(PlanErrors.NONE_ACTIVE);
            }

            var plan = active.get();
            var archived = plan.archive(now);
            if (archived.isFailure()) {
                return archived;
            }

            plans.update(plan);

            return Result.success();
        });
    }
}
