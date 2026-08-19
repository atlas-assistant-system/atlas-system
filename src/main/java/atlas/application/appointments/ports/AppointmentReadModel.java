package atlas.application.appointments.ports;

import atlas.application.sharedkernel.paging.Page;
import atlas.application.sharedkernel.paging.PageRequest;
import atlas.domain.appointments.AppointmentId;
import atlas.domain.appointments.vos.BookedSlot;
import atlas.domain.appointments.vos.TimeSlot;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AppointmentReadModel {

    Page<AppointmentSummary> findInWindow(TimeSlot window, boolean includeCancelled, PageRequest page);

    List<MonthlyAppointmentCount> countByMonth(TimeSlot yearWindow, boolean includeCancelled);

    List<DailyAppointmentCount> countByDay(TimeSlot monthWindow, boolean includeCancelled);

    List<AppointmentSummary> findOverlapping(TimeSlot candidate, Optional<AppointmentId> exclude);

    List<BookedSlot> findBookedSlots(TimeSlot window, Optional<AppointmentId> exclude);

    List<AppointmentSummary> findUpcoming(LocalDateTime now, int limit);

    List<DueReminder> findDueReminders(LocalDateTime now);
}
