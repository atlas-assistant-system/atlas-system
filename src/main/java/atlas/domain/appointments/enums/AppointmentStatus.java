package atlas.domain.appointments.enums;

public enum AppointmentStatus {

    SCHEDULED,
    CANCELLED;

    public boolean isActive() {
        return this == SCHEDULED;
    }

    public boolean isCancelled() {
        return this == CANCELLED;
    }
}
