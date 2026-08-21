package atlas.application.nutrition.ports;

import atlas.domain.nutrition.WeighIn;
import atlas.domain.nutrition.WeighInId;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WeighInReadModel {

    Optional<WeighIn> find(WeighInId id);

    List<WeighIn> findBetween(LocalDate from, LocalDate to);

    Optional<WeighIn> findLatest();
}
