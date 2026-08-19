package atlas.application.appointments.queries.findoverlappingappointments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import atlas.application.appointments.ports.AppointmentReadModel;
import atlas.application.appointments.ports.AppointmentSummary;
import atlas.domain.appointments.AppointmentErrors;
import atlas.domain.appointments.AppointmentId;
import atlas.domain.appointments.enums.AppointmentStatus;
import atlas.domain.appointments.vos.AppointmentTitle;
import atlas.domain.appointments.vos.TimeSlot;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class FindOverlappingAppointmentsQueryHandlerTest {

    private static final LocalDateTime TEN = LocalDateTime.of(2026, 8, 17, 10, 0);
    private static final LocalDateTime ELEVEN = LocalDateTime.of(2026, 8, 17, 11, 0);

    private final AppointmentReadModel appointments = mock(AppointmentReadModel.class);
    private final FindOverlappingAppointmentsQueryHandler handler =
        new FindOverlappingAppointmentsQueryHandler(appointments);

    @Test
    void shouldFailWhenTheRangeIsInverted() {
        var result = handler.handle(new FindOverlappingAppointmentsQuery(ELEVEN, TEN, Optional.empty()));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error()).isEqualTo(AppointmentErrors.INVALID_TIME_SLOT);
        verifyNoInteractions(appointments);
    }

    @Test
    void shouldReturnTheSummariesMappedToDtosWhenAppointmentsOverlap() {
        when(appointments.findOverlapping(TimeSlot.of(TEN, ELEVEN), Optional.empty()))
            .thenReturn(List.of(summary()));

        var result = handler.handle(new FindOverlappingAppointmentsQuery(TEN, ELEVEN, Optional.empty()));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value()).singleElement().satisfies(dto -> {
            assertThat(dto.id()).isEqualTo("A00000001");
            assertThat(dto.title()).isEqualTo("Dentista");
            assertThat(dto.status()).isEqualTo("SCHEDULED");
        });
    }

    @Test
    void shouldForwardTheExcludedAppointmentIdToTheReadModel() {
        var exclude = Optional.of(AppointmentId.of(7));
        when(appointments.findOverlapping(TimeSlot.of(TEN, ELEVEN), exclude)).thenReturn(List.of());

        var result = handler.handle(new FindOverlappingAppointmentsQuery(TEN, ELEVEN, exclude));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value()).isEmpty();
        verify(appointments).findOverlapping(TimeSlot.of(TEN, ELEVEN), exclude);
    }

    private static AppointmentSummary summary() {
        return new AppointmentSummary(
            AppointmentId.of(1),
            AppointmentTitle.create("Dentista").value(),
            TimeSlot.of(LocalDateTime.of(2026, 8, 17, 10, 30), LocalDateTime.of(2026, 8, 17, 11, 30)),
            AppointmentStatus.SCHEDULED);
    }
}
