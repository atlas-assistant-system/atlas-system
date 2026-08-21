package atlas.application.nutrition.ports;

import atlas.application.sharedkernel.ports.Repository;
import atlas.domain.nutrition.WeighIn;
import atlas.domain.nutrition.WeighInId;
import java.time.LocalDate;
import java.util.Optional;

public interface WeighInRepository extends Repository<WeighIn, WeighInId> {

    WeighInId nextId();

    Optional<WeighIn> findOn(LocalDate date);
}
