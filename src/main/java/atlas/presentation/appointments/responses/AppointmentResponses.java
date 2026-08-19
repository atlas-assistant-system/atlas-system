package atlas.presentation.appointments.responses;

import atlas.application.appointments.dto.AppointmentDto;
import atlas.application.appointments.dto.AppointmentPageDto;
import atlas.application.appointments.dto.AppointmentSummaryDto;
import atlas.application.appointments.dto.DailyAppointmentCountDto;
import atlas.application.appointments.dto.DueReminderDto;
import atlas.application.appointments.dto.FreeSlotDto;
import atlas.application.appointments.dto.MonthlyAppointmentCountDto;
import atlas.application.appointments.dto.ReminderDto;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AppointmentResponses {

    private AppointmentResponses() {}

    public static Map<String, Object> appointment(AppointmentDto dto) {
        var body = new LinkedHashMap<String, Object>();
        body.put("id", dto.id());
        body.put("title", dto.title());
        body.put("description", dto.description());
        body.put("start", dto.start().toString());
        body.put("end", dto.end().toString());
        body.put("status", dto.status());
        body.put("reminders", dto.reminders().stream().map(AppointmentResponses::reminder).toList());
        body.put("conflictingAppointmentIds", dto.conflictingAppointmentIds());

        return body;
    }

    public static Map<String, Object> reminder(ReminderDto dto) {
        var body = new LinkedHashMap<String, Object>();
        body.put("id", dto.id());
        body.put("leadTimeMinutes", dto.leadTimeMinutes());
        body.put("acknowledgedAt", dto.acknowledgedAt() == null ? null : dto.acknowledgedAt().toString());

        return body;
    }

    public static Map<String, Object> summary(AppointmentSummaryDto dto) {
        var body = new LinkedHashMap<String, Object>();
        body.put("id", dto.id());
        body.put("title", dto.title());
        body.put("start", dto.start().toString());
        body.put("end", dto.end().toString());
        body.put("status", dto.status());

        return body;
    }

    public static Map<String, Object> page(AppointmentPageDto dto) {
        var body = new LinkedHashMap<String, Object>();
        body.put("items", dto.page().items().stream().map(AppointmentResponses::summary).toList());
        body.put("pageNumber", dto.page().pageNumber());
        body.put("pageSize", dto.page().pageSize());
        body.put("totalCount", dto.page().totalCount());
        body.put("totalPages", dto.page().totalPages());
        body.put("anchor", dto.anchor().toString());
        body.put("previousAnchor", dto.previousAnchor().toString());
        body.put("nextAnchor", dto.nextAnchor().toString());

        return body;
    }

    public static List<Map<String, Object>> monthlyCounts(List<MonthlyAppointmentCountDto> counts) {
        return counts.stream().map(count -> {
            var body = new LinkedHashMap<String, Object>();
            body.put("month", count.month().toString());
            body.put("count", count.count());

            return (Map<String, Object>) body;
        }).toList();
    }

    public static List<Map<String, Object>> dailyCounts(List<DailyAppointmentCountDto> counts) {
        return counts.stream().map(count -> {
            var body = new LinkedHashMap<String, Object>();
            body.put("day", count.day().toString());
            body.put("count", count.count());

            return (Map<String, Object>) body;
        }).toList();
    }

    public static List<Map<String, Object>> summaries(List<AppointmentSummaryDto> summaries) {
        return summaries.stream().map(AppointmentResponses::summary).toList();
    }

    public static List<Map<String, Object>> freeSlots(List<FreeSlotDto> slots) {
        return slots.stream().map(slot -> {
            var body = new LinkedHashMap<String, Object>();
            body.put("start", slot.start().toString());
            body.put("end", slot.end().toString());

            return (Map<String, Object>) body;
        }).toList();
    }

    public static List<Map<String, Object>> dueReminders(List<DueReminderDto> reminders) {
        return reminders.stream().map(AppointmentResponses::dueReminder).toList();
    }

    public static Map<String, Object> dueReminder(DueReminderDto dto) {
        var body = new LinkedHashMap<String, Object>();
        body.put("appointmentId", dto.appointmentId());
        body.put("reminderId", dto.reminderId());
        body.put("title", dto.title());
        body.put("start", dto.start().toString());
        body.put("leadTimeMinutes", dto.leadTimeMinutes());

        return body;
    }
}
