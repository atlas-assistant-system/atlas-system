package atlas.application.appointments.queries.listappointmentsbyperiod;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import atlas.application.appointments.ports.AppointmentReadModel;
import atlas.application.appointments.ports.AppointmentSummary;
import atlas.application.sharedkernel.paging.Page;
import atlas.application.sharedkernel.paging.PageRequest;
import atlas.domain.appointments.AppointmentId;
import atlas.domain.appointments.enums.AppointmentStatus;
import atlas.domain.appointments.enums.CalendarPeriod;
import atlas.domain.appointments.vos.AppointmentTitle;
import atlas.domain.appointments.vos.TimeSlot;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class ListAppointmentsByPeriodQueryHandlerTest {

    private static final LocalDate WEDNESDAY = LocalDate.of(2026, 8, 19);
    private static final PageRequest FIRST_PAGE = PageRequest.of(1, 20);

    private final AppointmentReadModel appointments = mock(AppointmentReadModel.class);
    private final ListAppointmentsByPeriodQueryHandler handler =
        new ListAppointmentsByPeriodQueryHandler(appointments);

    @Test
    void shouldReturnThePageMappedToDtos() {
        var window = CalendarPeriod.WEEK.windowFor(WEDNESDAY);
        when(appointments.findInWindow(window, false, FIRST_PAGE))
            .thenReturn(Page.of(List.of(summary()), FIRST_PAGE, 1));

        var result = handler.handle(new ListAppointmentsByPeriodQuery(CalendarPeriod.WEEK, WEDNESDAY, false,
            FIRST_PAGE));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value().page().items()).singleElement().satisfies(dto -> {
            assertThat(dto.id()).isEqualTo("A00000001");
            assertThat(dto.title()).isEqualTo("Dentista");
            assertThat(dto.status()).isEqualTo("SCHEDULED");
        });
        assertThat(result.value().page().totalCount()).isEqualTo(1);
    }

    @Test
    void shouldComputeTheNavigationAnchorsFromThePeriod() {
        var window = CalendarPeriod.WEEK.windowFor(WEDNESDAY);
        when(appointments.findInWindow(window, false, FIRST_PAGE)).thenReturn(Page.empty(FIRST_PAGE));

        var result = handler.handle(new ListAppointmentsByPeriodQuery(CalendarPeriod.WEEK, WEDNESDAY, false,
            FIRST_PAGE));

        assertThat(result.value().anchor()).isEqualTo(WEDNESDAY);
        assertThat(result.value().previousAnchor()).isEqualTo(LocalDate.of(2026, 8, 10));
        assertThat(result.value().nextAnchor()).isEqualTo(LocalDate.of(2026, 8, 24));
    }

    @Test
    void shouldAskTheReadModelForTheWindowOfThePeriod() {
        when(appointments.findInWindow(CalendarPeriod.MONTH.windowFor(WEDNESDAY), true, FIRST_PAGE))
            .thenReturn(Page.empty(FIRST_PAGE));

        var result = handler.handle(new ListAppointmentsByPeriodQuery(CalendarPeriod.MONTH, WEDNESDAY, true,
            FIRST_PAGE));

        assertThat(result.isSuccess()).isTrue();
        verify(appointments).findInWindow(CalendarPeriod.MONTH.windowFor(WEDNESDAY), true, FIRST_PAGE);
    }

    private static AppointmentSummary summary() {
        return new AppointmentSummary(
            AppointmentId.of(1),
            AppointmentTitle.create("Dentista").value(),
            TimeSlot.of(LocalDateTime.of(2026, 8, 19, 10, 0), LocalDateTime.of(2026, 8, 19, 11, 0)),
            AppointmentStatus.SCHEDULED);
    }
}
