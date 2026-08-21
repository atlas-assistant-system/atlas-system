package atlas.application.nutrition.queries.getday;

import atlas.application.nutrition.dto.DayDto;
import atlas.application.nutrition.mappers.NutritionMapper;
import atlas.application.nutrition.ports.IntakeReadModel;
import atlas.application.nutrition.ports.PlanReadModel;
import atlas.application.sharedkernel.cqrs.QueryHandler;
import atlas.domain.nutrition.Intake;
import atlas.domain.nutrition.Plan;
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
        var consumed = totalOf(eaten);

        return plans.findActive()
            .map(plan -> withQuota(date, consumed, eaten, plan))
            .orElseGet(() -> Result.success(NutritionMapper.toDto(date, consumed, eaten)));
    }

    private static Result<DayDto> withQuota(
        LocalDate date, Macros consumed, Collection<Intake> eaten, Plan plan) {

        return Result.success(
            NutritionMapper.toDto(date, new DayTotals(consumed, plan.dailyMacros()), eaten));
    }

    private static Macros totalOf(Collection<Intake> eaten) {
        return eaten.stream().map(Intake::macros).reduce(Macros.NONE, Macros::plus);
    }
}
