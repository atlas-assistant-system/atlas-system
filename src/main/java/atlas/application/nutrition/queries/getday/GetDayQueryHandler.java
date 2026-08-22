package atlas.application.nutrition.queries.getday;

import atlas.application.nutrition.dto.DayDto;
import atlas.application.nutrition.mappers.NutritionMapper;
import atlas.application.nutrition.ports.IntakeReadModel;
import atlas.application.nutrition.ports.PlanReadModel;
import atlas.application.sharedkernel.cqrs.QueryHandler;
import atlas.domain.nutrition.Intake;
import atlas.domain.nutrition.Plan;
import atlas.domain.nutrition.vos.Calories;
import atlas.domain.nutrition.vos.DayTotals;
import atlas.domain.nutrition.vos.Macros;
import atlas.domain.sharedkernel.results.Result;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Collection;

public final class GetDayQueryHandler implements QueryHandler<GetDayQuery, Result<DayDto>> {

    private final IntakeReadModel intakes;
    private final PlanReadModel plans;
    private final Clock clock;

    public GetDayQueryHandler(IntakeReadModel intakes, PlanReadModel plans, Clock clock) {
        this.intakes = intakes;
        this.plans = plans;
        this.clock = clock;
    }

    @Override
    public Result<DayDto> handle(GetDayQuery query) {
        var date = query.date() == null ? LocalDate.now(clock) : query.date();
        var eaten = intakes.findOn(date);
        var calories = caloriesOf(eaten);
        var macros = macrosOf(eaten);

        return plans.findActive()
            .map(plan -> withQuota(date, calories, macros, eaten, plan))
            .orElseGet(() -> Result.success(
                NutritionMapper.toDto(date, calories, macros, eaten)));
    }

    private static Result<DayDto> withQuota(
        LocalDate date, Calories calories, Macros macros, Collection<Intake> eaten, Plan plan) {

        var totals = new DayTotals(macros, calories, plan.dailyMacros(), plan.dailyCalories());

        return Result.success(NutritionMapper.toDto(date, totals, eaten));
    }

    private static Calories caloriesOf(Collection<Intake> eaten) {
        return eaten.stream().map(Intake::calories).reduce(Calories.NONE, Calories::plus);
    }

    private static Macros macrosOf(Collection<Intake> eaten) {
        return eaten.stream().map(Intake::macros).reduce(Macros.NONE, Macros::plus);
    }
}
