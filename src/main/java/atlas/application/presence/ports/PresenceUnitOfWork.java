package atlas.application.presence.ports;

import atlas.application.sharedkernel.unitofwork.UnitOfWork;

public interface PresenceUnitOfWork extends UnitOfWork {

    BiometricProfileRepository profiles();

    AuthenticationSessionRepository sessions();

    AuthenticationGateRepository gate();
}
