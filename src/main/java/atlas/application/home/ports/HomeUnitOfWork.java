package atlas.application.home.ports;

import atlas.application.sharedkernel.unitofwork.UnitOfWork;

public interface HomeUnitOfWork extends UnitOfWork {

    HomeProfileRepository profiles();
}
