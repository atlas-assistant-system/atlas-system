package atlas.domain.appointments.services;

import atlas.domain.appointments.AppointmentErrors;
import atlas.domain.appointments.AppointmentId;
import atlas.domain.appointments.vos.BookedSlot;
import atlas.domain.appointments.vos.TimeSlot;
import atlas.domain.sharedkernel.results.Result;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class AppointmentAvailability {

    public List<AppointmentId> findConflicts(TimeSlot candidate, List<BookedSlot> booked) {
        return booked.stream()
            .filter(bookedSlot -> bookedSlot.slot().overlaps(candidate))
            .map(BookedSlot::appointmentId)
            .toList();
    }

    public Result<List<AppointmentId>> ensureSchedulable(
        TimeSlot candidate, List<BookedSlot> booked, boolean allowOverlap) {

        var conflicts = findConflicts(candidate, booked);
        if (!conflicts.isEmpty() && !allowOverlap) {
            return Result.failure(AppointmentErrors.overlaps(conflicts));
        }

        return Result.success(conflicts);
    }

    public List<TimeSlot> findFreeSlots(TimeSlot searchWindow, Duration minDuration, List<BookedSlot> booked) {
        var occupied = booked.stream()
            .map(BookedSlot::slot)
            .filter(slot -> slot.overlaps(searchWindow))
            .sorted(Comparator.comparing(TimeSlot::start))
            .toList();

        var free = new ArrayList<TimeSlot>();
        var cursor = searchWindow.start();

        for (var slot : occupied) {
            if (slot.start().isAfter(cursor)) {
                addIfLongEnough(free, cursor, slot.start(), minDuration);
            }

            if (slot.end().isAfter(cursor)) {
                cursor = slot.end();
            }
        }

        addIfLongEnough(free, cursor, searchWindow.end(), minDuration);
        return List.copyOf(free);
    }

    private void addIfLongEnough(List<TimeSlot> free, LocalDateTime start, LocalDateTime end, Duration minDuration) {
        if (!end.isAfter(start)) {
            return;
        }

        if (Duration.between(start, end).compareTo(minDuration) >= 0) {
            free.add(TimeSlot.of(start, end));
        }
    }
}
