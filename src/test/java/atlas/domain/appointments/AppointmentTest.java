package atlas.domain.appointments;

import static org.assertj.core.api.Assertions.assertThat;

import atlas.domain.appointments.entities.Reminder;
import atlas.domain.appointments.entities.ReminderId;
import atlas.domain.appointments.enums.AppointmentStatus;
import atlas.domain.appointments.events.AppointmentCancelledEvent;
import atlas.domain.appointments.events.AppointmentDeletedEvent;
import atlas.domain.appointments.events.AppointmentDetailsChangedEvent;
import atlas.domain.appointments.events.AppointmentRescheduledEvent;
import atlas.domain.appointments.events.AppointmentRestoredEvent;
import atlas.domain.appointments.events.AppointmentScheduledEvent;
import atlas.domain.appointments.events.ReminderAcknowledgedEvent;
import atlas.domain.appointments.events.ReminderAddedEvent;
import atlas.domain.appointments.events.ReminderRemovedEvent;
import atlas.domain.appointments.vos.AppointmentDescription;
import atlas.domain.appointments.vos.AppointmentTitle;
import atlas.domain.appointments.vos.ReminderLeadTime;
import atlas.domain.appointments.vos.TimeSlot;
import atlas.domain.sharedkernel.results.Result;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AppointmentTest {

    private static final AppointmentId ID = AppointmentId.of(1);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 17, 10, 0);
    private static final LocalDateTime AFTER_THE_SLOT_ENDED = LocalDateTime.of(2026, 8, 17, 13, 0);
    private static final Instant OCCURRED_ON = Instant.parse("2026-08-17T10:00:00Z");
    private static final Instant LATER = Instant.parse("2026-08-17T10:30:00Z");

    private static final TimeSlot FUTURE_SLOT =
        TimeSlot.of(LocalDateTime.of(2026, 8, 17, 11, 0), LocalDateTime.of(2026, 8, 17, 12, 0));
    private static final TimeSlot ANOTHER_FUTURE_SLOT =
        TimeSlot.of(LocalDateTime.of(2026, 8, 17, 15, 0), LocalDateTime.of(2026, 8, 17, 16, 0));
    private static final TimeSlot PAST_SLOT =
        TimeSlot.of(LocalDateTime.of(2026, 8, 17, 8, 0), LocalDateTime.of(2026, 8, 17, 9, 0));

    @Test
    void shouldScheduleAppointmentWhenSlotIsInTheFuture() {
        var result = schedule(FUTURE_SLOT);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value().status()).isEqualTo(AppointmentStatus.SCHEDULED);
        assertThat(result.value().timeSlot()).isEqualTo(FUTURE_SLOT);
    }

    @Test
    void shouldRaiseScheduledEventWhenAppointmentIsCreated() {
        var appointment = schedule(FUTURE_SLOT).value();

        assertThat(appointment.pendingEvents())
            .singleElement()
            .isInstanceOfSatisfying(AppointmentScheduledEvent.class, event -> {
                assertThat(event.appointmentId()).isEqualTo(ID);
                assertThat(event.timeSlot()).isEqualTo(FUTURE_SLOT);
                assertThat(event.occurredOn()).isEqualTo(OCCURRED_ON);
            });
    }

    @Test
    void shouldFailWhenSchedulingInThePast() {
        var result = schedule(PAST_SLOT);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error()).isEqualTo(AppointmentErrors.CANNOT_SCHEDULE_IN_THE_PAST);
    }

    @Test
    void shouldStartWithoutRemindersBecauseTheyAreOptional() {
        assertThat(schedule(FUTURE_SLOT).value().reminders()).isEmpty();
    }

    @Test
    void shouldExposeRemindersAsUnmodifiable() {
        assertThat(scheduled().reminders()).isUnmodifiable();
    }

    @Test
    void shouldRescheduleWhenAppointmentIsActiveAndSlotIsInTheFuture() {
        var appointment = scheduled();

        var result = appointment.reschedule(ANOTHER_FUTURE_SLOT, NOW, OCCURRED_ON);

        assertThat(result.isSuccess()).isTrue();
        assertThat(appointment.timeSlot()).isEqualTo(ANOTHER_FUTURE_SLOT);
        assertThat(appointment.pendingEvents())
            .singleElement()
            .isInstanceOfSatisfying(AppointmentRescheduledEvent.class, event -> {
                assertThat(event.appointmentId()).isEqualTo(ID);
                assertThat(event.newTimeSlot()).isEqualTo(ANOTHER_FUTURE_SLOT);
                assertThat(event.occurredOn()).isEqualTo(OCCURRED_ON);
            });
    }

    @Test
    void shouldFailWhenReschedulingToThePast() {
        var appointment = scheduled();

        var result = appointment.reschedule(PAST_SLOT, NOW, OCCURRED_ON);

        assertThat(result.error()).isEqualTo(AppointmentErrors.CANNOT_SCHEDULE_IN_THE_PAST);
        assertThat(appointment.timeSlot()).isEqualTo(FUTURE_SLOT);
    }

    @Test
    void shouldFailWhenReschedulingACancelledAppointment() {
        var result = cancelled().reschedule(ANOTHER_FUTURE_SLOT, NOW, OCCURRED_ON);

        assertThat(result.error()).isEqualTo(AppointmentErrors.CANNOT_MODIFY_CANCELLED);
    }

    @Test
    void shouldFailWhenReschedulingAnAppointmentThatAlreadyHappened() {
        var result = scheduled().reschedule(ANOTHER_FUTURE_SLOT, AFTER_THE_SLOT_ENDED, OCCURRED_ON);

        assertThat(result.error()).isEqualTo(AppointmentErrors.CANNOT_MODIFY_PAST_APPOINTMENT);
    }

    @Test
    void shouldCancelWithoutDeletingTheAppointment() {
        var appointment = scheduled();

        var result = appointment.cancel(NOW, OCCURRED_ON);

        assertThat(result.isSuccess()).isTrue();
        assertThat(appointment.status()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(appointment.timeSlot()).isEqualTo(FUTURE_SLOT);
        assertThat(appointment.pendingEvents())
            .singleElement()
            .isInstanceOfSatisfying(AppointmentCancelledEvent.class, event -> {
                assertThat(event.appointmentId()).isEqualTo(ID);
                assertThat(event.occurredOn()).isEqualTo(OCCURRED_ON);
            });
    }

    @Test
    void shouldFailWhenCancellingTwice() {
        var result = cancelled().cancel(NOW, OCCURRED_ON);

        assertThat(result.error()).isEqualTo(AppointmentErrors.CANNOT_MODIFY_CANCELLED);
    }

    @Test
    void shouldAnnounceItsDeletion() {
        var appointment = scheduled();

        appointment.delete(LATER);

        assertThat(appointment.pendingEvents())
            .singleElement()
            .isInstanceOfSatisfying(AppointmentDeletedEvent.class, event -> {
                assertThat(event.appointmentId()).isEqualTo(ID);
                assertThat(event.occurredOn()).isEqualTo(LATER);
            });
    }

    @Test
    void shouldRestoreACancelledAppointmentKeepingItsSlot() {
        var appointment = cancelled();

        var result = appointment.restore(NOW, LATER);

        assertThat(result.isSuccess()).isTrue();
        assertThat(appointment.status()).isEqualTo(AppointmentStatus.SCHEDULED);
        assertThat(appointment.timeSlot()).isEqualTo(FUTURE_SLOT);
        assertThat(appointment.pendingEvents())
            .singleElement()
            .isInstanceOfSatisfying(AppointmentRestoredEvent.class, event -> {
                assertThat(event.appointmentId()).isEqualTo(ID);
                assertThat(event.timeSlot()).isEqualTo(FUTURE_SLOT);
                assertThat(event.occurredOn()).isEqualTo(LATER);
            });
    }

    @Test
    void shouldKeepTheRemindersOfARestoredAppointment() {
        var appointment = cancelled();
        appointment.restore(NOW, LATER);

        var result = appointment.addReminder(reminderId(1), leadTime(15), NOW, LATER);

        assertThat(result.isSuccess()).isTrue();
        assertThat(appointment.reminders()).hasSize(1);
    }

    @Test
    void shouldFailWhenRestoringAnAppointmentThatIsNotCancelled() {
        var result = scheduled().restore(NOW, OCCURRED_ON);

        assertThat(result.error()).isEqualTo(AppointmentErrors.NOT_CANCELLED);
    }

    @Test
    void shouldFailWhenRestoringAnAppointmentThatAlreadyStarted() {
        var appointment = cancelled();

        var result = appointment.restore(AFTER_THE_SLOT_ENDED, OCCURRED_ON);

        assertThat(result.error()).isEqualTo(AppointmentErrors.CANNOT_RESTORE_PAST_APPOINTMENT);
        assertThat(appointment.status()).isEqualTo(AppointmentStatus.CANCELLED);
    }

    @Test
    void shouldChangeDetailsWhenAppointmentIsActive() {
        var appointment = scheduled();

        var result = appointment.changeDetails(title("Revision"), description("Traer informe"), OCCURRED_ON);

        assertThat(result.isSuccess()).isTrue();
        assertThat(appointment.title()).isEqualTo(title("Revision"));
        assertThat(appointment.description()).map(AppointmentDescription::value).contains("Traer informe");
        assertThat(appointment.pendingEvents())
            .singleElement()
            .isInstanceOfSatisfying(AppointmentDetailsChangedEvent.class, event -> {
                assertThat(event.appointmentId()).isEqualTo(ID);
                assertThat(event.title()).isEqualTo(title("Revision"));
                assertThat(event.description()).map(AppointmentDescription::value).contains("Traer informe");
                assertThat(event.occurredOn()).isEqualTo(OCCURRED_ON);
            });
    }

    @Test
    void shouldAllowChangingDetailsOfAnAppointmentThatAlreadyHappened() {
        var appointment = alreadyHappened();

        var result = appointment.changeDetails(title("Dentista con el Dr. Ruiz"), Optional.empty(), OCCURRED_ON);

        assertThat(result.isSuccess()).isTrue();
        assertThat(appointment.title()).isEqualTo(title("Dentista con el Dr. Ruiz"));
    }

    @Test
    void shouldClearDescriptionWhenNoneIsGiven() {
        var appointment = scheduled();
        appointment.changeDetails(title("Con nota"), description("algo"), OCCURRED_ON);

        appointment.changeDetails(title("Sin nota"), Optional.empty(), OCCURRED_ON);

        assertThat(appointment.description()).isEmpty();
    }

    @Test
    void shouldFailWhenChangingDetailsOfACancelledAppointment() {
        var result = cancelled().changeDetails(title("Otro"), Optional.empty(), OCCURRED_ON);

        assertThat(result.error()).isEqualTo(AppointmentErrors.CANNOT_MODIFY_CANCELLED);
    }

    @Test
    void shouldAddReminderWhenAppointmentIsUpcoming() {
        var appointment = scheduled();

        var result = appointment.addReminder(reminderId(1), leadTime(15), NOW, OCCURRED_ON);

        assertThat(result.isSuccess()).isTrue();
        assertThat(appointment.reminders()).hasSize(1);
        assertThat(appointment.pendingEvents())
            .singleElement()
            .isInstanceOfSatisfying(ReminderAddedEvent.class, event -> {
                assertThat(event.appointmentId()).isEqualTo(ID);
                assertThat(event.reminderId()).isEqualTo(reminderId(1));
                assertThat(event.leadTime()).isEqualTo(leadTime(15));
                assertThat(event.occurredOn()).isEqualTo(OCCURRED_ON);
            });
    }

    @Test
    void shouldRejectASecondReminderWithTheSameLeadTime() {
        var appointment = scheduled();
        appointment.addReminder(reminderId(1), leadTime(15), NOW, OCCURRED_ON);

        var result = appointment.addReminder(reminderId(2), leadTime(15), NOW, OCCURRED_ON);

        assertThat(result.error()).isEqualTo(AppointmentErrors.DUPLICATE_REMINDER_LEAD_TIME);
        assertThat(appointment.reminders()).hasSize(1);
    }

    @Test
    void shouldRejectMoreRemindersThanTheMaximum() {
        var appointment = scheduled();
        for (var i = 0; i < Appointment.MAX_REMINDERS; i++) {
            appointment.addReminder(reminderId(i), leadTime(i + 1), NOW, OCCURRED_ON);
        }

        var result = appointment.addReminder(reminderId(99), leadTime(120), NOW, OCCURRED_ON);

        assertThat(result.error()).isEqualTo(AppointmentErrors.TOO_MANY_REMINDERS);
        assertThat(appointment.reminders()).hasSize(Appointment.MAX_REMINDERS);
    }

    @Test
    void shouldRejectAReminderForAnAppointmentThatAlreadyHappened() {
        var result = scheduled().addReminder(reminderId(1), leadTime(15), AFTER_THE_SLOT_ENDED, OCCURRED_ON);

        assertThat(result.error()).isEqualTo(AppointmentErrors.CANNOT_ADD_REMINDER_TO_PAST_APPOINTMENT);
    }

    @Test
    void shouldRemoveAnExistingReminder() {
        var appointment = withReminder();

        var result = appointment.removeReminder(reminderId(1), OCCURRED_ON);

        assertThat(result.isSuccess()).isTrue();
        assertThat(appointment.reminders()).isEmpty();
        assertThat(appointment.pendingEvents())
            .singleElement()
            .isInstanceOfSatisfying(ReminderRemovedEvent.class, event -> {
                assertThat(event.appointmentId()).isEqualTo(ID);
                assertThat(event.reminderId()).isEqualTo(reminderId(1));
                assertThat(event.occurredOn()).isEqualTo(OCCURRED_ON);
            });
    }

    @Test
    void shouldTouchOnlyTheTargetedReminderWhenSeveralCoexist() {
        var appointment = withReminder();
        appointment.addReminder(reminderId(2), leadTime(60), NOW, OCCURRED_ON);
        appointment.clearEvents();

        appointment.acknowledgeReminder(reminderId(2), LATER);
        appointment.removeReminder(reminderId(1), LATER);

        assertThat(appointment.reminders())
            .singleElement()
            .satisfies(reminder -> {
                assertThat(reminder.id()).isEqualTo(reminderId(2));
                assertThat(reminder.acknowledgedAt()).contains(LATER);
            });
    }

    @Test
    void shouldFailWhenRemovingAReminderThatIsNotThere() {
        var result = scheduled().removeReminder(reminderId(1), OCCURRED_ON);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error().code()).isEqualTo("Appointment.ReminderNotFound");
    }

    @Test
    void shouldAcknowledgeAReminderOnce() {
        var appointment = withReminder();

        var result = appointment.acknowledgeReminder(reminderId(1), OCCURRED_ON);

        assertThat(result.isSuccess()).isTrue();
        assertThat(appointment.reminders().getFirst().acknowledgedAt()).contains(OCCURRED_ON);
        assertThat(appointment.pendingEvents())
            .singleElement()
            .isInstanceOfSatisfying(ReminderAcknowledgedEvent.class, event -> {
                assertThat(event.appointmentId()).isEqualTo(ID);
                assertThat(event.reminderId()).isEqualTo(reminderId(1));
                assertThat(event.occurredOn()).isEqualTo(OCCURRED_ON);
            });
    }

    @Test
    void shouldNotAnnounceAgainWhenReminderIsAcknowledgedTwice() {
        var appointment = withReminder();
        appointment.acknowledgeReminder(reminderId(1), OCCURRED_ON);
        appointment.clearEvents();

        var result = appointment.acknowledgeReminder(reminderId(1), LATER);

        assertThat(result.isSuccess()).isTrue();
        assertThat(appointment.pendingEvents()).isEmpty();
    }

    @Test
    void shouldFailWhenAcknowledgingAReminderThatIsNotThere() {
        var result = scheduled().acknowledgeReminder(reminderId(1), OCCURRED_ON);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error().code()).isEqualTo("Appointment.ReminderNotFound");
    }

    @Test
    void shouldRestoreItsRemindersWhenRehydrated() {
        var reminder = Reminder.create(reminderId(1), leadTime(15));

        var appointment = Appointment.rehydrate(
            ID, title("Dentista"), description("Traer informe"), FUTURE_SLOT, AppointmentStatus.CANCELLED,
            List.of(reminder));

        assertThat(appointment.reminders()).containsExactly(reminder);
        assertThat(appointment.status()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(appointment.description()).map(AppointmentDescription::value).contains("Traer informe");
    }

    @Test
    void shouldNotRaiseAnyEventWhenRehydrated() {
        assertThat(alreadyHappened().pendingEvents()).isEmpty();
    }

    @Test
    void shouldExposeItselfAsABookedSlot() {
        var bookedSlot = scheduled().toBookedSlot();

        assertThat(bookedSlot.appointmentId()).isEqualTo(ID);
        assertThat(bookedSlot.slot()).isEqualTo(FUTURE_SLOT);
    }

    private static Result<Appointment> schedule(TimeSlot slot) {
        return Appointment.schedule(ID, title("Dentista"), Optional.empty(), slot, NOW, OCCURRED_ON);
    }

    private static Appointment scheduled() {
        var appointment = schedule(FUTURE_SLOT).value();
        appointment.clearEvents();

        return appointment;
    }

    private static Appointment withReminder() {
        var appointment = scheduled();
        appointment.addReminder(reminderId(1), leadTime(15), NOW, OCCURRED_ON);
        appointment.clearEvents();

        return appointment;
    }

    private static Appointment cancelled() {
        var appointment = scheduled();
        appointment.cancel(NOW, OCCURRED_ON);
        appointment.clearEvents();

        return appointment;
    }

    private static Appointment alreadyHappened() {
        return Appointment.rehydrate(
            ID, title("Dentista"), Optional.empty(), PAST_SLOT, AppointmentStatus.SCHEDULED, List.of());
    }

    private static AppointmentTitle title(String value) {
        return AppointmentTitle.create(value).value();
    }

    private static Optional<AppointmentDescription> description(String value) {
        return AppointmentDescription.create(value).value();
    }

    private static ReminderLeadTime leadTime(int minutes) {
        return ReminderLeadTime.create(minutes).value();
    }

    private static ReminderId reminderId(int seed) {
        return ReminderId.of(new UUID(0, seed));
    }
}
