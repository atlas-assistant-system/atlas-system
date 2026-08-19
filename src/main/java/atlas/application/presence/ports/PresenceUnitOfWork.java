package atlas.application.presence.ports;

import sharedkernel.application.unitofwork.UnitOfWork;

public interface PresenceUnitOfWork extends UnitOfWork {

    BiometricProfileRepository profiles();

    AuthenticationSessionRepository sessions();

    AuthenticationGateRepository gate();
}
