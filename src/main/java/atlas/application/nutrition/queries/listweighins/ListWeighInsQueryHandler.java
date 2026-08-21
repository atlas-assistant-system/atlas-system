package atlas.application.nutrition.queries.listweighins;

import atlas.application.nutrition.dto.WeighInDto;
import atlas.application.nutrition.mappers.NutritionMapper;
import atlas.application.nutrition.ports.WeighInReadModel;
import atlas.application.nutrition.queries.Period;
import atlas.application.sharedkernel.cqrs.QueryHandler;
import atlas.domain.sharedkernel.results.Result;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

public final class ListWeighInsQueryHandler
    implements QueryHandler<ListWeighInsQuery, Result<List<WeighInDto>>> {

    public static final int DEFAULT_DAYS = 90;

    private final WeighInReadModel weighIns;
    private final Clock clock;

    public ListWeighInsQueryHandler(WeighInReadModel weighIns, Clock clock) {
        this.weighIns = weighIns;
        this.clock = clock;
    }

    @Override
    public Result<List<WeighInDto>> handle(ListWeighInsQuery query) {
        var today = LocalDate.now(clock);
        var period = query.from() == null && query.to() == null
            ? new Period(today.minusDays(DEFAULT_DAYS - 1L), today)
            : Period.of(query.from(), query.to(), today);

        return Result.success(NutritionMapper.toWeighInDtos(
            weighIns.findBetween(period.from(), period.to())));
    }
}
