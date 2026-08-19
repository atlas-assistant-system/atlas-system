package atlas.presentation.appointments.sse;

import atlas.application.sharedkernel.events.SimpleDomainEventPublisher;
import atlas.domain.appointments.events.AppointmentCancelledEvent;
import atlas.domain.appointments.events.AppointmentDeletedEvent;
import atlas.domain.appointments.events.AppointmentDetailsChangedEvent;
import atlas.domain.appointments.events.AppointmentRescheduledEvent;
import atlas.domain.appointments.events.AppointmentRestoredEvent;
import atlas.domain.appointments.events.AppointmentScheduledEvent;
import atlas.domain.appointments.events.ReminderAcknowledgedEvent;
import atlas.domain.appointments.events.ReminderAddedEvent;
import atlas.domain.appointments.events.ReminderRemovedEvent;
import atlas.presentation.common.web.Json;
import atlas.presentation.sharedkernel.sse.SseEvent;
import atlas.presentation.sharedkernel.sse.SseHub;
import java.util.LinkedHashMap;
import java.util.Map;

public final class AppointmentEventsBroadcaster {

    private AppointmentEventsBroadcaster() {}

    public static void subscribeAll(SimpleDomainEventPublisher events, SseHub hub) {
        events.subscribe(AppointmentScheduledEvent.class, event -> {
            var data = new LinkedHashMap<String, Object>();
            data.put("appointmentId", event.appointmentId().toString());
            data.put("start", event.timeSlot().start().toString());
            data.put("end", event.timeSlot().end().toString());
            hub.broadcast(SseEvent.named("appointmentScheduled", Json.write(data)));
        });

        events.subscribe(AppointmentRescheduledEvent.class, event -> {
            var data = new LinkedHashMap<String, Object>();
            data.put("appointmentId", event.appointmentId().toString());
            data.put("start", event.newTimeSlot().start().toString());
            data.put("end", event.newTimeSlot().end().toString());
            hub.broadcast(SseEvent.named("appointmentRescheduled", Json.write(data)));
        });

        events.subscribe(AppointmentRestoredEvent.class, event -> {
            var data = new LinkedHashMap<String, Object>();
            data.put("appointmentId", event.appointmentId().toString());
            data.put("start", event.timeSlot().start().toString());
            data.put("end", event.timeSlot().end().toString());
            hub.broadcast(SseEvent.named("appointmentRestored", Json.write(data)));
        });

        events.subscribe(AppointmentCancelledEvent.class, event -> hub.broadcast(SseEvent.named(
            "appointmentCancelled", Json.write(Map.of("appointmentId", event.appointmentId().toString())))));

        events.subscribe(AppointmentDeletedEvent.class, event -> hub.broadcast(SseEvent.named(
            "appointmentDeleted", Json.write(Map.of("appointmentId", event.appointmentId().toString())))));

        events.subscribe(AppointmentDetailsChangedEvent.class, event -> hub.broadcast(SseEvent.named(
            "appointmentDetailsChanged", Json.write(Map.of("appointmentId", event.appointmentId().toString())))));

        events.subscribe(ReminderAddedEvent.class, event -> hub.broadcast(SseEvent.named(
            "reminderAdded",
            Json.write(reminderData(event.appointmentId().toString(), event.reminderId().toString())))));

        events.subscribe(ReminderRemovedEvent.class, event -> hub.broadcast(SseEvent.named(
            "reminderRemoved",
            Json.write(reminderData(event.appointmentId().toString(), event.reminderId().toString())))));

        events.subscribe(ReminderAcknowledgedEvent.class, event -> hub.broadcast(SseEvent.named(
            "reminderAcknowledged",
            Json.write(reminderData(event.appointmentId().toString(), event.reminderId().toString())))));
    }

    private static LinkedHashMap<String, Object> reminderData(String appointmentId, String reminderId) {
        var data = new LinkedHashMap<String, Object>();
        data.put("appointmentId", appointmentId);
        data.put("reminderId", reminderId);

        return data;
    }
}
