package atlas.application.nutrition.ports;

import atlas.application.sharedkernel.ports.Repository;
import atlas.domain.nutrition.Intake;
import atlas.domain.nutrition.IntakeId;

public interface IntakeRepository extends Repository<Intake, IntakeId> {

    IntakeId nextId();
}
