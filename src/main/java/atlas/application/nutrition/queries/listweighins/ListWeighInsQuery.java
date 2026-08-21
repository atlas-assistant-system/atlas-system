package atlas.application.nutrition.queries.listweighins;

import atlas.application.nutrition.dto.WeighInDto;
import atlas.application.sharedkernel.cqrs.Query;
import atlas.domain.sharedkernel.results.Result;
import java.time.LocalDate;
import java.util.List;

public record ListWeighInsQuery(LocalDate from, LocalDate to) implements Query<Result<List<WeighInDto>>> {}
