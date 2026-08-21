package atlas.application.nutrition.queries.listintakes;

import atlas.application.nutrition.dto.IntakeDto;
import atlas.application.nutrition.mappers.NutritionMapper;
import atlas.application.nutrition.ports.IntakeReadModel;
import atlas.application.nutrition.queries.Period;
import atlas.application.sharedkernel.cqrs.QueryHandler;
import atlas.domain.sharedkernel.results.Result;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

public final class ListIntakesQueryHandler implements QueryHandler<ListIntakesQuery, Result<List<IntakeDto>>> {

    public static final int DEFAULT_LIMIT = 50;
    public static final int MAX_LIMIT = 500;

    private final IntakeReadModel intakes;
    private final Clock clock;

    public ListIntakesQueryHandler(IntakeReadModel intakes, Clock clock) {
        this.intakes = intakes;
        this.clock = clock;
    }

    @Override
    public Result<List<IntakeDto>> handle(ListIntakesQuery query) {
        var period = Period.of(query.from(), query.to(), LocalDate.now(clock));
        var found = intakes.findBetween(period.from(), period.to(), limitOf(query.limit()));

        return Result.success(NutritionMapper.toDtos(found));
    }

    private static int limitOf(Integer requested) {
        if (requested == null || requested < 1) {
            return DEFAULT_LIMIT;
        }

        return Math.min(requested, MAX_LIMIT);
    }
}
