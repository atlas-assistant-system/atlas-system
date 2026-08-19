package atlas.application.appointments.ports;

import atlas.application.sharedkernel.unitofwork.UnitOfWork;

public interface AppointmentUnitOfWork extends UnitOfWork {

    AppointmentRepository appointments();
}
