package atlas.application.appointments.queries.getduereminders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import atlas.application.appointments.ports.AppointmentReadModel;
import atlas.application.appointments.ports.DueReminder;
import atlas.domain.appointments.AppointmentId;
import atlas.domain.appointments.entities.ReminderId;
import atlas.domain.appointments.vos.AppointmentTitle;
import atlas.domain.appointments.vos.ReminderLeadTime;
import atlas.domain.appointments.vos.TimeSlot;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetDueRemindersQueryHandlerTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 17, 9, 50);
    private static final UUID REMINDER_UUID = UUID.fromString("6f9e6f0e-2f4b-4a4e-9d2c-8b1a3c5d7e9f");

    private final AppointmentReadModel appointments = mock(AppointmentReadModel.class);
    private final GetDueRemindersQueryHandler handler = new GetDueRemindersQueryHandler(appointments);

    @Test
    void shouldMapEachDueReminderToItsDto() {
        when(appointments.findDueReminders(NOW)).thenReturn(List.of(dueReminder()));

        var result = handler.handle(new GetDueRemindersQuery(NOW));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value()).singleElement().satisfies(dto -> {
            assertThat(dto.appointmentId()).isEqualTo("A00000001");
            assertThat(dto.reminderId()).isEqualTo(REMINDER_UUID.toString());
            assertThat(dto.title()).isEqualTo("Dentista");
            assertThat(dto.start()).isEqualTo(LocalDateTime.of(2026, 8, 17, 10, 0));
            assertThat(dto.leadTimeMinutes()).isEqualTo(15);
        });
    }

    @Test
    void shouldReturnAnEmptyListWhenTheReadModelHasNoDueReminders() {
        when(appointments.findDueReminders(NOW)).thenReturn(List.of());

        var result = handler.handle(new GetDueRemindersQuery(NOW));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value()).isEmpty();
    }

    @Test
    void shouldForwardNowVerbatimToTheReadModel() {
        when(appointments.findDueReminders(NOW)).thenReturn(List.of());

        var result = handler.handle(new GetDueRemindersQuery(NOW));

        assertThat(result.isSuccess()).isTrue();
        verify(appointments).findDueReminders(NOW);
    }

    private static DueReminder dueReminder() {
        return new DueReminder(
            AppointmentId.of(1),
            ReminderId.of(REMINDER_UUID),
            AppointmentTitle.create("Dentista").value(),
            TimeSlot.of(LocalDateTime.of(2026, 8, 17, 10, 0), LocalDateTime.of(2026, 8, 17, 11, 0)),
            ReminderLeadTime.create(15).value());
    }
}
