package atlas.application.nutrition.ports;

import atlas.domain.nutrition.Intake;
import atlas.domain.nutrition.IntakeId;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface IntakeReadModel {

    Optional<Intake> find(IntakeId id);

    List<Intake> findOn(LocalDate date);

    List<Intake> findBetween(LocalDate from, LocalDate to, int limit);

    List<DayConsumption> consumptionBetween(LocalDate from, LocalDate to);
}
