package atlas.domain.appointments.services;

import static org.assertj.core.api.Assertions.assertThat;

import atlas.domain.appointments.AppointmentId;
import atlas.domain.appointments.vos.BookedSlot;
import atlas.domain.appointments.vos.TimeSlot;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class AppointmentAvailabilityTest {

    private static final LocalDate DAY = LocalDate.of(2026, 8, 17);
    private static final TimeSlot WORKING_HOURS = slot(9, 18);
    private static final Duration ONE_HOUR = Duration.ofHours(1);

    private final AppointmentAvailability availability = new AppointmentAvailability();

    @Test
    void shouldFindNoConflictsWhenNothingIsBooked() {
        var conflicts = availability.findConflicts(slot(10, 11), List.of());

        assertThat(conflicts).isEmpty();
    }

    @Test
    void shouldFindTheAppointmentWhoseSlotOverlaps() {
        var booked = List.of(booking(1, slot(10, 11)), booking(2, slot(15, 16)));

        var conflicts = availability.findConflicts(slot(10, 12), booked);

        assertThat(conflicts).containsExactly(AppointmentId.of(1));
    }

    @Test
    void shouldFindEveryOverlappingAppointment() {
        var booked = List.of(booking(1, slot(10, 11)), booking(2, slot(12, 13)));

        var conflicts = availability.findConflicts(slot(10, 13), booked);

        assertThat(conflicts).containsExactly(AppointmentId.of(1), AppointmentId.of(2));
    }

    @Test
    void shouldNotReportConflictForBackToBackAppointments() {
        var booked = List.of(booking(1, slot(10, 11)));

        var conflicts = availability.findConflicts(slot(11, 12), booked);

        assertThat(conflicts).isEmpty();
    }

    @Test
    void shouldAllowSchedulingWhenNothingConflicts() {
        var result = availability.ensureSchedulable(slot(10, 11), List.of(), false);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value()).isEmpty();
    }

    @Test
    void shouldBlockSchedulingWhenConflictsExistAndOverlapIsNotAllowed() {
        var booked = List.of(booking(1, slot(10, 12)));

        var result = availability.ensureSchedulable(slot(11, 13), booked, false);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error().code()).isEqualTo("Appointment.Overlaps");
        assertThat(result.error().message()).contains("A00000001");
    }

    @Test
    void shouldAllowSchedulingOverConflictsWhenOverlapIsExplicitlyAllowed() {
        var booked = List.of(booking(1, slot(10, 12)));

        var result = availability.ensureSchedulable(slot(11, 13), booked, true);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value()).containsExactly(AppointmentId.of(1));
    }

    @Test
    void shouldReturnTheWholeWindowWhenNothingIsBooked() {
        var free = availability.findFreeSlots(WORKING_HOURS, ONE_HOUR, List.of());

        assertThat(free).containsExactly(WORKING_HOURS);
    }

    @Test
    void shouldReturnTheGapsBetweenBookings() {
        var booked = List.of(booking(1, slot(10, 11)), booking(2, slot(13, 14)));

        var free = availability.findFreeSlots(WORKING_HOURS, ONE_HOUR, booked);

        assertThat(free).containsExactly(slot(9, 10), slot(11, 13), slot(14, 18));
    }

    @Test
    void shouldDiscardGapsShorterThanTheRequestedDuration() {
        var booked = List.of(booking(1, slot(10, 11)), booking(2, slot(13, 14)));

        var free = availability.findFreeSlots(WORKING_HOURS, Duration.ofMinutes(90), booked);

        assertThat(free).containsExactly(slot(11, 13), slot(14, 18));
    }

    @Test
    void shouldTreatOverlappingBookingsAsOneBusyStretch() {
        var booked = List.of(booking(1, slot(10, 12)), booking(2, slot(11, 13)));

        var free = availability.findFreeSlots(WORKING_HOURS, ONE_HOUR, booked);

        assertThat(free).containsExactly(slot(9, 10), slot(13, 18));
    }

    @Test
    void shouldTreatANestedBookingAsPartOfTheEnclosingOne() {
        var booked = List.of(booking(1, slot(10, 14)), booking(2, slot(11, 12)));

        var free = availability.findFreeSlots(WORKING_HOURS, ONE_HOUR, booked);

        assertThat(free).containsExactly(slot(9, 10), slot(14, 18));
    }

    @Test
    void shouldIgnoreBookingsOutsideTheSearchWindow() {
        var booked = List.of(booking(1, slot(6, 7)), booking(2, slot(20, 21)));

        var free = availability.findFreeSlots(WORKING_HOURS, ONE_HOUR, booked);

        assertThat(free).containsExactly(WORKING_HOURS);
    }

    @Test
    void shouldReturnNothingWhenTheWindowIsFullyBooked() {
        var booked = List.of(booking(1, WORKING_HOURS));

        var free = availability.findFreeSlots(WORKING_HOURS, ONE_HOUR, booked);

        assertThat(free).isEmpty();
    }

    @Test
    void shouldClipABookingThatStartsBeforeTheWindow() {
        var booked = List.of(booking(1, slot(8, 10)));

        var free = availability.findFreeSlots(WORKING_HOURS, ONE_HOUR, booked);

        assertThat(free).containsExactly(slot(10, 18));
    }

    @Test
    void shouldNotDependOnTheOrderBookingsArriveIn() {
        var ascending = List.of(booking(1, slot(10, 11)), booking(2, slot(13, 14)));
        var descending = List.of(booking(2, slot(13, 14)), booking(1, slot(10, 11)));

        assertThat(availability.findFreeSlots(WORKING_HOURS, ONE_HOUR, ascending))
            .isEqualTo(availability.findFreeSlots(WORKING_HOURS, ONE_HOUR, descending));
    }

    @Test
    void shouldReturnAnUnmodifiableResult() {
        var free = availability.findFreeSlots(WORKING_HOURS, ONE_HOUR, List.of());

        assertThat(free).isUnmodifiable();
    }

    private static TimeSlot slot(int startHour, int endHour) {
        return TimeSlot.of(
            LocalDateTime.of(DAY, LocalTime.of(startHour, 0)), LocalDateTime.of(DAY, LocalTime.of(endHour, 0)));
    }

    private static BookedSlot booking(long id, TimeSlot slot) {
        return BookedSlot.of(AppointmentId.of(id), slot);
    }
}
