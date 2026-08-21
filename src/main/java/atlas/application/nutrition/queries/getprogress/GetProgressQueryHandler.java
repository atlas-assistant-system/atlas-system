package atlas.application.nutrition.queries.getprogress;

import atlas.application.nutrition.dto.ProgressDto;
import atlas.application.nutrition.mappers.NutritionMapper;
import atlas.application.nutrition.ports.PlanReadModel;
import atlas.application.nutrition.ports.WeighInReadModel;
import atlas.application.sharedkernel.cqrs.QueryHandler;
import atlas.domain.nutrition.PlanErrors;
import atlas.domain.nutrition.WeighIn;
import atlas.domain.nutrition.services.PlanProgress;
import atlas.domain.sharedkernel.results.Result;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

public final class GetProgressQueryHandler implements QueryHandler<GetProgressQuery, Result<ProgressDto>> {

    private static final int TREND_DAYS = 2 * PlanProgress.TREND_WINDOW_DAYS;

    private final PlanReadModel plans;
    private final WeighInReadModel weighIns;
    private final PlanProgress progress;
    private final Clock clock;

    public GetProgressQueryHandler(
        PlanReadModel plans, WeighInReadModel weighIns, PlanProgress progress, Clock clock) {
        this.plans = plans;
        this.weighIns = weighIns;
        this.progress = progress;
        this.clock = clock;
    }

    @Override
    public Result<ProgressDto> handle(GetProgressQuery query) {
        var active = plans.findActive();
        if (active.isEmpty()) {
            return Result.failure(PlanErrors.NONE_ACTIVE);
        }

        var today = LocalDate.now(clock);

        return Result.success(NutritionMapper.toDto(
            progress.of(active.get(), seriesFor(today), today)));
    }

    private List<WeighIn> seriesFor(LocalDate today) {
        var lastTwoWeeks = weighIns.findBetween(today.minusDays(TREND_DAYS - 1L), today);

        return lastTwoWeeks.isEmpty() ? lastKnownReading() : lastTwoWeeks;
    }

    private List<WeighIn> lastKnownReading() {
        return weighIns.findLatest().map(List::of).orElseGet(List::of);
    }
}
