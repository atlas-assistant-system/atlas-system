package atlas.application.nutrition.ports;

import atlas.application.sharedkernel.ports.Repository;
import atlas.domain.nutrition.Plan;
import atlas.domain.nutrition.PlanId;
import java.util.Optional;

public interface PlanRepository extends Repository<Plan, PlanId> {

    PlanId nextId();

    Optional<Plan> findActive();
}
