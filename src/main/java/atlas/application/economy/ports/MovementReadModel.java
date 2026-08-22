package atlas.application.economy.ports;

import atlas.domain.economy.Movement;
import atlas.domain.economy.MovementId;
import atlas.domain.economy.enums.Category;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MovementReadModel {

    Optional<Movement> find(MovementId id);

    List<Movement> findLatest(LocalDate from, LocalDate to, Category category, int limit);

    Balance balanceBetween(LocalDate from, LocalDate to);

    List<CategorySpend> spendingBetween(LocalDate from, LocalDate to);
}
