package atlas.domain.appointments.vos;

import atlas.domain.appointments.AppointmentId;
import atlas.domain.sharedkernel.ddd.ValueObject;
import atlas.domain.sharedkernel.guards.ObjectGuard;

public record BookedSlot(AppointmentId appointmentId, TimeSlot slot) implements ValueObject {

    public BookedSlot {
        ObjectGuard.notNull(appointmentId, "appointmentId");
        ObjectGuard.notNull(slot, "slot");
    }

    public static BookedSlot of(AppointmentId appointmentId, TimeSlot slot) {
        return new BookedSlot(appointmentId, slot);
    }
}
