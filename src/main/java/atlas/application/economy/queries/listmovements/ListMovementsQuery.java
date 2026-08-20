package atlas.application.economy.queries.listmovements;

import atlas.application.economy.dto.MovementDto;
import atlas.application.sharedkernel.cqrs.Query;
import atlas.domain.economy.enums.Category;
import atlas.domain.sharedkernel.results.Result;
import java.time.LocalDate;
import java.util.List;

public record ListMovementsQuery(
    LocalDate from,
    LocalDate to,
    Category category,
    Integer limit) implements Query<Result<List<MovementDto>>> {}
