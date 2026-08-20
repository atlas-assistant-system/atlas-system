package atlas.application.economy.ports;

import atlas.application.sharedkernel.unitofwork.UnitOfWork;

public interface EconomyUnitOfWork extends UnitOfWork {

    MovementRepository movements();
}
