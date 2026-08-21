package atlas.application.nutrition.ports;

import atlas.domain.nutrition.Plan;
import java.util.Optional;

public interface PlanReadModel {

    Optional<Plan> findActive();
}
