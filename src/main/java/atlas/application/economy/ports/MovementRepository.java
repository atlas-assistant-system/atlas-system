package atlas.application.economy.ports;

import atlas.application.sharedkernel.ports.Repository;
import atlas.domain.economy.Movement;
import atlas.domain.economy.MovementId;

public interface MovementRepository extends Repository<Movement, MovementId> {

    MovementId nextId();
}
