package atlas.application.nutrition.queries.listdays;

import atlas.application.nutrition.dto.DaySummaryDto;
import atlas.application.nutrition.mappers.NutritionMapper;
import atlas.application.nutrition.ports.IntakeReadModel;
import atlas.application.nutrition.ports.PlanReadModel;
import atlas.application.nutrition.queries.Period;
import atlas.application.sharedkernel.cqrs.QueryHandler;
import atlas.domain.nutrition.Plan;
import atlas.domain.nutrition.vos.Calories;
import atlas.domain.sharedkernel.results.Result;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

public final class ListDaysQueryHandler implements QueryHandler<ListDaysQuery, Result<List<DaySummaryDto>>> {

    private static final Calories NO_QUOTA = new Calories(0);

    private final IntakeReadModel intakes;
    private final PlanReadModel plans;
    private final Clock clock;

    public ListDaysQueryHandler(IntakeReadModel intakes, PlanReadModel plans, Clock clock) {
        this.intakes = intakes;
        this.plans = plans;
        this.clock = clock;
    }

    @Override
    public Result<List<DaySummaryDto>> handle(ListDaysQuery query) {
        var period = Period.of(query.from(), query.to(), LocalDate.now(clock));
        var consumption = intakes.consumptionBetween(period.from(), period.to());
        var dailyCalories = plans.findActive().map(Plan::dailyCalories).orElse(NO_QUOTA);

        return Result.success(NutritionMapper.toDays(consumption, dailyCalories));
    }
}
