package atlas.application.economy.ports;

import atlas.application.sharedkernel.unitofwork.UnitOfWork;

public interface MovementUnitOfWork extends UnitOfWork {

    MovementRepository movements();
}
