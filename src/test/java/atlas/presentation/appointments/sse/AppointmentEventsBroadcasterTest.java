package atlas.presentation.appointments.sse;

import static org.assertj.core.api.Assertions.assertThat;

import atlas.application.sharedkernel.events.SimpleDomainEventPublisher;
import atlas.domain.appointments.AppointmentId;
import atlas.domain.appointments.entities.ReminderId;
import atlas.domain.appointments.events.AppointmentDeletedEvent;
import atlas.domain.appointments.events.AppointmentRestoredEvent;
import atlas.domain.appointments.events.AppointmentScheduledEvent;
import atlas.domain.appointments.events.ReminderAcknowledgedEvent;
import atlas.domain.appointments.vos.TimeSlot;
import atlas.presentation.sharedkernel.sse.SseHub;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AppointmentEventsBroadcasterTest {

    private static final Instant OCCURRED_ON = Instant.parse("2026-08-17T08:00:00Z");

    private final SimpleDomainEventPublisher events = new SimpleDomainEventPublisher();
    private final SseHub hub = new SseHub();
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();

    @Test
    void shouldBroadcastAScheduledEventWithItsPayload() {
        hub.register(output);
        AppointmentEventsBroadcaster.subscribeAll(events, hub);
        var slot = TimeSlot.of(LocalDateTime.of(2026, 8, 20, 11, 0), LocalDateTime.of(2026, 8, 20, 12, 0));

        events.publish(new AppointmentScheduledEvent(AppointmentId.of(7), slot, OCCURRED_ON));

        var wire = output.toString(StandardCharsets.UTF_8);
        assertThat(wire).contains("event: appointmentScheduled");
        assertThat(wire).contains("\"appointmentId\":\"A00000007\"");
        assertThat(wire).contains("\"start\":\"2026-08-20T11:00\"");
    }

    @Test
    void shouldBroadcastARestoredEventWithTheRecoveredSlot() {
        hub.register(output);
        AppointmentEventsBroadcaster.subscribeAll(events, hub);
        var slot = TimeSlot.of(LocalDateTime.of(2026, 8, 20, 11, 0), LocalDateTime.of(2026, 8, 20, 12, 0));

        events.publish(new AppointmentRestoredEvent(AppointmentId.of(7), slot, OCCURRED_ON));

        var wire = output.toString(StandardCharsets.UTF_8);
        assertThat(wire).contains("event: appointmentRestored");
        assertThat(wire).contains("\"appointmentId\":\"A00000007\"");
        assertThat(wire).contains("\"start\":\"2026-08-20T11:00\"");
        assertThat(wire).contains("\"end\":\"2026-08-20T12:00\"");
    }

    @Test
    void shouldBroadcastADeletionSoOpenViewsDropTheAppointment() {
        hub.register(output);
        AppointmentEventsBroadcaster.subscribeAll(events, hub);

        events.publish(new AppointmentDeletedEvent(AppointmentId.of(7), OCCURRED_ON));

        var wire = output.toString(StandardCharsets.UTF_8);
        assertThat(wire).contains("event: appointmentDeleted");
        assertThat(wire).contains("\"appointmentId\":\"A00000007\"");
    }

    @Test
    void shouldBroadcastAReminderAcknowledgementWithBothIds() {
        hub.register(output);
        AppointmentEventsBroadcaster.subscribeAll(events, hub);
        var reminderId = ReminderId.of(new UUID(0, 1));

        events.publish(new ReminderAcknowledgedEvent(AppointmentId.of(7), reminderId, OCCURRED_ON));

        var wire = output.toString(StandardCharsets.UTF_8);
        assertThat(wire).contains("event: reminderAcknowledged");
        assertThat(wire).contains("\"appointmentId\":\"A00000007\"");
        assertThat(wire).contains("\"reminderId\":\"" + reminderId + "\"");
    }
}
