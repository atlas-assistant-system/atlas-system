package atlas.domain.appointments.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AppointmentStatusTest {

    @Test
    void shouldConsiderOnlyScheduledAsActive() {
        assertThat(AppointmentStatus.SCHEDULED.isActive()).isTrue();
        assertThat(AppointmentStatus.CANCELLED.isActive()).isFalse();
    }

    @Test
    void shouldRoundTripThroughItsName() {
        assertThat(AppointmentStatus.valueOf("SCHEDULED")).isEqualTo(AppointmentStatus.SCHEDULED);
        assertThat(AppointmentStatus.CANCELLED.name()).isEqualTo("CANCELLED");
    }
}
