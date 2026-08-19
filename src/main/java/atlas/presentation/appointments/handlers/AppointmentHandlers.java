package atlas.presentation.appointments.handlers;

import atlas.application.appointments.commands.acknowledgereminder.AcknowledgeReminderCommand;
import atlas.application.appointments.commands.addreminder.AddReminderCommand;
import atlas.application.appointments.commands.cancelappointment.CancelAppointmentCommand;
import atlas.application.appointments.commands.changeappointmentdetails.ChangeAppointmentDetailsCommand;
import atlas.application.appointments.commands.deleteappointment.DeleteAppointmentCommand;
import atlas.application.appointments.commands.removereminder.RemoveReminderCommand;
import atlas.application.appointments.commands.rescheduleappointment.RescheduleAppointmentCommand;
import atlas.application.appointments.commands.restoreappointment.RestoreAppointmentCommand;
import atlas.application.appointments.commands.scheduleappointment.ScheduleAppointmentCommand;
import atlas.application.appointments.dto.AppointmentDto;
import atlas.application.appointments.queries.findfreeslots.FindFreeSlotsQuery;
import atlas.application.appointments.queries.findoverlappingappointments.FindOverlappingAppointmentsQuery;
import atlas.application.appointments.queries.getappointment.GetAppointmentQuery;
import atlas.application.appointments.queries.getappointmentcountsbyday.GetAppointmentCountsByDayQuery;
import atlas.application.appointments.queries.getappointmentcountsbymonth.GetAppointmentCountsByMonthQuery;
import atlas.application.appointments.queries.getduereminders.GetDueRemindersQuery;
import atlas.application.appointments.queries.getupcomingappointments.GetUpcomingAppointmentsQuery;
import atlas.application.appointments.queries.listappointmentsbyperiod.ListAppointmentsByPeriodQuery;
import atlas.application.sharedkernel.cqrs.CommandBus;
import atlas.application.sharedkernel.cqrs.QueryBus;
import atlas.application.sharedkernel.paging.PageRequest;
import atlas.domain.appointments.AppointmentId;
import atlas.domain.appointments.entities.ReminderId;
import atlas.domain.appointments.enums.CalendarPeriod;
import atlas.domain.sharedkernel.exceptions.FormatException;
import atlas.domain.sharedkernel.results.Result;
import atlas.presentation.appointments.requests.AddReminderRequest;
import atlas.presentation.appointments.requests.ChangeAppointmentDetailsRequest;
import atlas.presentation.appointments.requests.RescheduleAppointmentRequest;
import atlas.presentation.appointments.requests.RestoreAppointmentRequest;
import atlas.presentation.appointments.requests.ScheduleAppointmentRequest;
import atlas.presentation.appointments.responses.AppointmentResponses;
import atlas.presentation.appointments.web.Values;
import atlas.presentation.common.web.Json;
import atlas.presentation.sharedkernel.http.HttpRequest;
import atlas.presentation.sharedkernel.http.HttpResponse;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

public final class AppointmentHandlers {

    private final CommandBus commands;
    private final QueryBus queries;
    private final Clock clock;

    public AppointmentHandlers(CommandBus commands, QueryBus queries, Clock clock) {
        this.commands = commands;
        this.queries = queries;
        this.clock = clock;
    }

    public HttpResponse schedule(HttpRequest request) {
        var body = ScheduleAppointmentRequest.from(Json.parse(request.body()));

        Result<AppointmentDto> result = commands.dispatch(new ScheduleAppointmentCommand(
            body.title(), body.description(), body.start(), body.end(), body.reminderLeadTimesMinutes(),
            body.allowOverlap()));
        if (result.isFailure()) {
            return HttpResponse.error(result.error());
        }

        return HttpResponse.created(
            "/appointments/" + result.value().id(), Json.write(AppointmentResponses.appointment(result.value())));
    }

    public HttpResponse reschedule(HttpRequest request) {
        var body = RescheduleAppointmentRequest.from(Json.parse(request.body()));

        Result<AppointmentDto> result = commands.dispatch(new RescheduleAppointmentCommand(
            appointmentId(request), body.newStart(), body.newEnd(), body.allowOverlap()));

        return okOrError(result);
    }

    public HttpResponse cancel(HttpRequest request) {
        Result<Void> result = commands.dispatch(new CancelAppointmentCommand(appointmentId(request)));
        if (result.isFailure()) {
            return HttpResponse.error(result.error());
        }

        return HttpResponse.noContent();
    }

    public HttpResponse delete(HttpRequest request) {
        Result<Void> result = commands.dispatch(new DeleteAppointmentCommand(appointmentId(request)));
        if (result.isFailure()) {
            return HttpResponse.error(result.error());
        }

        return HttpResponse.noContent();
    }

    public HttpResponse restore(HttpRequest request) {
        var body = RestoreAppointmentRequest.from(Json.parseOptional(request.body()));

        Result<AppointmentDto> result = commands.dispatch(new RestoreAppointmentCommand(
            appointmentId(request), body.allowOverlap()));

        return okOrError(result);
    }

    public HttpResponse changeDetails(HttpRequest request) {
        var body = ChangeAppointmentDetailsRequest.from(Json.parse(request.body()));

        Result<AppointmentDto> result = commands.dispatch(new ChangeAppointmentDetailsCommand(
            appointmentId(request), body.title(), body.description()));

        return okOrError(result);
    }

    public HttpResponse addReminder(HttpRequest request) {
        var body = AddReminderRequest.from(Json.parse(request.body()));

        Result<AppointmentDto> result = commands.dispatch(new AddReminderCommand(
            appointmentId(request), body.leadTimeMinutes()));

        return okOrError(result);
    }

    public HttpResponse removeReminder(HttpRequest request) {
        Result<Void> result = commands.dispatch(new RemoveReminderCommand(
            appointmentId(request), reminderId(request)));
        if (result.isFailure()) {
            return HttpResponse.error(result.error());
        }

        return HttpResponse.noContent();
    }

    public HttpResponse acknowledgeReminder(HttpRequest request) {
        Result<Void> result = commands.dispatch(new AcknowledgeReminderCommand(
            appointmentId(request), reminderId(request)));
        if (result.isFailure()) {
            return HttpResponse.error(result.error());
        }

        return HttpResponse.noContent();
    }

    public HttpResponse detail(HttpRequest request) {
        Result<AppointmentDto> result = queries.dispatch(new GetAppointmentQuery(appointmentId(request)));

        return okOrError(result);
    }

    public HttpResponse list(HttpRequest request) {
        var period = Values.parseEnum(request.queryParam("period").orElse("DAY"), CalendarPeriod.class, "period");
        var anchor = Values.parseDate(
            request.queryParam("anchor").orElse(LocalDateTime.now(clock).toLocalDate().toString()), "anchor");
        var page = PageRequest.of(
            Values.parseInteger(request.queryParam("page"), "page", 1),
            Values.parseInteger(request.queryParam("pageSize"), "pageSize", 20));

        var result = queries.dispatch(new ListAppointmentsByPeriodQuery(
            period, anchor, Values.parseFlag(request.queryParam("includeCancelled")), page));
        if (result.isFailure()) {
            return HttpResponse.error(result.error());
        }

        return HttpResponse.ok(Json.write(AppointmentResponses.page(result.value())));
    }

    public HttpResponse countsByMonth(HttpRequest request) {
        var year = Values.parseYear(request.queryParam("year").orElse(null), "year");

        var result = queries.dispatch(new GetAppointmentCountsByMonthQuery(
            year, Values.parseFlag(request.queryParam("includeCancelled"))));
        if (result.isFailure()) {
            return HttpResponse.error(result.error());
        }

        return HttpResponse.ok(Json.write(AppointmentResponses.monthlyCounts(result.value())));
    }

    public HttpResponse countsByDay(HttpRequest request) {
        var month = Values.parseYearMonth(request.queryParam("month").orElse(null), "month");

        var result = queries.dispatch(new GetAppointmentCountsByDayQuery(
            month, Values.parseFlag(request.queryParam("includeCancelled"))));
        if (result.isFailure()) {
            return HttpResponse.error(result.error());
        }

        return HttpResponse.ok(Json.write(AppointmentResponses.dailyCounts(result.value())));
    }

    public HttpResponse overlapping(HttpRequest request) {
        var start = Values.parseDateTime(request.queryParam("start").orElse(null), "start");
        var end = Values.parseDateTime(request.queryParam("end").orElse(null), "end");
        var exclude = request.queryParam("exclude").map(AppointmentId::parse);

        var result = queries.dispatch(new FindOverlappingAppointmentsQuery(start, end, exclude));
        if (result.isFailure()) {
            return HttpResponse.error(result.error());
        }

        return HttpResponse.ok(Json.write(AppointmentResponses.summaries(result.value())));
    }

    public HttpResponse freeSlots(HttpRequest request) {
        var day = Values.parseDate(request.queryParam("day").orElse(null), "day");
        var from = Values.parseTime(request.queryParam("from").orElse("09:00"), "from");
        var to = Values.parseTime(request.queryParam("to").orElse("18:00"), "to");
        var minDuration = Values.parseInteger(request.queryParam("minDurationMinutes"), "minDurationMinutes", 30);

        var result = queries.dispatch(new FindFreeSlotsQuery(day, from, to, minDuration));
        if (result.isFailure()) {
            return HttpResponse.error(result.error());
        }

        return HttpResponse.ok(Json.write(AppointmentResponses.freeSlots(result.value())));
    }

    public HttpResponse upcoming(HttpRequest request) {
        var limit = Values.parseInteger(request.queryParam("limit"), "limit", 5);

        var result = queries.dispatch(new GetUpcomingAppointmentsQuery(LocalDateTime.now(clock), limit));
        if (result.isFailure()) {
            return HttpResponse.error(result.error());
        }

        return HttpResponse.ok(Json.write(AppointmentResponses.summaries(result.value())));
    }

    public HttpResponse dueReminders(HttpRequest request) {
        var at = request.queryParam("at")
            .map(value -> Values.parseDateTime(value, "at"))
            .orElseGet(() -> LocalDateTime.now(clock));

        var result = queries.dispatch(new GetDueRemindersQuery(at));
        if (result.isFailure()) {
            return HttpResponse.error(result.error());
        }

        return HttpResponse.ok(Json.write(AppointmentResponses.dueReminders(result.value())));
    }

    private HttpResponse okOrError(Result<AppointmentDto> result) {
        if (result.isFailure()) {
            return HttpResponse.error(result.error());
        }

        return HttpResponse.ok(Json.write(AppointmentResponses.appointment(result.value())));
    }

    private static AppointmentId appointmentId(HttpRequest request) {
        return AppointmentId.parse(request.pathParam("id"));
    }

    private static ReminderId reminderId(HttpRequest request) {
        try {
            return ReminderId.of(UUID.fromString(request.pathParam("reminderId")));
        } catch (IllegalArgumentException e) {
            throw new FormatException("'reminderId' must be a UUID.");
        }
    }
}
