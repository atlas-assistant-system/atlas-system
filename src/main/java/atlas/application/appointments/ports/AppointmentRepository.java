package atlas.application.appointments.ports;

import atlas.application.sharedkernel.ports.Repository;
import atlas.domain.appointments.Appointment;
import atlas.domain.appointments.AppointmentId;
import atlas.domain.appointments.vos.TimeSlot;
import java.util.List;

public interface AppointmentRepository extends Repository<Appointment, AppointmentId> {

    AppointmentId nextId();

    List<Appointment> findActiveInWindow(TimeSlot window);
}
