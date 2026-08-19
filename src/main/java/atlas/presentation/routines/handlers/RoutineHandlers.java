package atlas.presentation.routines.handlers;

import atlas.application.routines.commands.archiveroutine.ArchiveRoutineCommand;
import atlas.application.routines.commands.changeroutinedetails.ChangeRoutineDetailsCommand;
import atlas.application.routines.commands.changeroutineschedule.ChangeRoutineScheduleCommand;
import atlas.application.routines.commands.clearday.ClearDayCommand;
import atlas.application.routines.commands.defineroutine.DefineRoutineCommand;
import atlas.application.routines.commands.deleteroutine.DeleteRoutineCommand;
import atlas.application.routines.commands.logprogress.LogProgressCommand;
import atlas.application.routines.commands.unarchiveroutine.UnarchiveRoutineCommand;
import atlas.application.routines.dto.PeriodProgressDto;
import atlas.application.routines.dto.RoutineDto;
import atlas.application.routines.queries.getcompliancestats.GetComplianceStatsQuery;
import atlas.application.routines.queries.gethistory.GetHistoryQuery;
import atlas.application.routines.queries.getperiodprogress.GetPeriodProgressQuery;
import atlas.application.routines.queries.getroutine.GetRoutineQuery;
import atlas.application.routines.queries.getstreak.GetStreakQuery;
import atlas.application.routines.queries.gettoday.GetTodayQuery;
import atlas.application.routines.queries.listroutines.ListRoutinesQuery;
import atlas.domain.routines.RoutineId;
import atlas.presentation.common.web.Json;
import atlas.presentation.routines.requests.ChangeRoutineDetailsRequest;
import atlas.presentation.routines.requests.ChangeRoutineScheduleRequest;
import atlas.presentation.routines.requests.DefineRoutineRequest;
import atlas.presentation.routines.requests.LogProgressRequest;
import atlas.presentation.routines.responses.RoutineResponses;
import atlas.presentation.routines.web.Values;
import java.time.Clock;
import java.time.LocalDate;
import sharedkernel.application.cqrs.CommandBus;
import sharedkernel.application.cqrs.QueryBus;
import sharedkernel.domain.results.Result;
import sharedkernel.presentation.http.HttpRequest;
import sharedkernel.presentation.http.HttpResponse;

public final class RoutineHandlers {

    private final CommandBus commands;
    private final QueryBus queries;
    private final Clock clock;

    public RoutineHandlers(CommandBus commands, QueryBus queries, Clock clock) {
        this.commands = commands;
        this.queries = queries;
        this.clock = clock;
    }

    public HttpResponse define(HttpRequest request) {
        var body = DefineRoutineRequest.from(Json.parse(request.body()));

        Result<RoutineDto> result = commands.dispatch(new DefineRoutineCommand(
            body.name(), body.description(), body.target(), body.unit(), body.period(),
            body.activeDays(), body.daysOfMonth()));
        if (result.isFailure()) {
            return HttpResponse.error(result.error());
        }

        return HttpResponse.created(
            "/routines/" + result.value().id(), Json.write(RoutineResponses.routine(result.value())));
    }

    public HttpResponse changeDetails(HttpRequest request) {
        var body = ChangeRoutineDetailsRequest.from(Json.parse(request.body()));

        Result<RoutineDto> result = commands.dispatch(new ChangeRoutineDetailsCommand(
            routineId(request), body.name(), body.description()));

        return okOrError(result);
    }

    public HttpResponse changeSchedule(HttpRequest request) {
        var body = ChangeRoutineScheduleRequest.from(Json.parse(request.body()));

        Result<RoutineDto> result = commands.dispatch(new ChangeRoutineScheduleCommand(
            routineId(request), body.target(), body.unit(), body.period(), body.activeDays(), body.daysOfMonth()));

        return okOrError(result);
    }

    public HttpResponse archive(HttpRequest request) {
        Result<Void> result = commands.dispatch(new ArchiveRoutineCommand(routineId(request)));

        return noContentOrError(result);
    }

    public HttpResponse unarchive(HttpRequest request) {
        Result<Void> result = commands.dispatch(new UnarchiveRoutineCommand(routineId(request)));

        return noContentOrError(result);
    }

    public HttpResponse delete(HttpRequest request) {
        Result<Void> result = commands.dispatch(new DeleteRoutineCommand(routineId(request)));

        return noContentOrError(result);
    }

    public HttpResponse logProgress(HttpRequest request) {
        var body = LogProgressRequest.from(Json.parseOptional(request.body()), LocalDate.now(clock));

        Result<PeriodProgressDto> result = commands.dispatch(new LogProgressCommand(
            routineId(request), body.day(), body.amount()));
        if (result.isFailure()) {
            return HttpResponse.error(result.error());
        }

        return HttpResponse.ok(Json.write(RoutineResponses.progress(result.value())));
    }

    public HttpResponse clearDay(HttpRequest request) {
        var day = Values.parseDate(request.pathParam("day"), "day");

        Result<Void> result = commands.dispatch(new ClearDayCommand(routineId(request), day));

        return noContentOrError(result);
    }

    public HttpResponse today(HttpRequest request) {
        var result = queries.dispatch(new GetTodayQuery());
        if (result.isFailure()) {
            return HttpResponse.error(result.error());
        }

        return HttpResponse.ok(Json.write(RoutineResponses.today(result.value())));
    }

    public HttpResponse list(HttpRequest request) {
        var result = queries.dispatch(new ListRoutinesQuery(
            Values.parseFlag(request.queryParam("includeArchived"))));
        if (result.isFailure()) {
            return HttpResponse.error(result.error());
        }

        return HttpResponse.ok(Json.write(RoutineResponses.summaries(result.value())));
    }

    public HttpResponse detail(HttpRequest request) {
        Result<RoutineDto> result = queries.dispatch(new GetRoutineQuery(routineId(request)));

        return okOrError(result);
    }

    public HttpResponse progress(HttpRequest request) {
        var day = request.queryParam("day")
            .map(value -> Values.parseDate(value, "day"))
            .orElseGet(() -> LocalDate.now(clock));

        var result = queries.dispatch(new GetPeriodProgressQuery(routineId(request), day));
        if (result.isFailure()) {
            return HttpResponse.error(result.error());
        }

        return HttpResponse.ok(Json.write(RoutineResponses.progress(result.value())));
    }

    public HttpResponse history(HttpRequest request) {
        var today = LocalDate.now(clock);
        var from = request.queryParam("from")
            .map(value -> Values.parseDate(value, "from"))
            .orElseGet(() -> today.minusDays(29));
        var to = request.queryParam("to").map(value -> Values.parseDate(value, "to")).orElse(today);

        var result = queries.dispatch(new GetHistoryQuery(routineId(request), from, to));
        if (result.isFailure()) {
            return HttpResponse.error(result.error());
        }

        return HttpResponse.ok(Json.write(RoutineResponses.history(result.value())));
    }

    public HttpResponse streak(HttpRequest request) {
        var result = queries.dispatch(new GetStreakQuery(routineId(request)));
        if (result.isFailure()) {
            return HttpResponse.error(result.error());
        }

        return HttpResponse.ok(Json.write(RoutineResponses.streak(result.value())));
    }

    public HttpResponse complianceStats(HttpRequest request) {
        var today = LocalDate.now(clock);
        var from = request.queryParam("from")
            .map(value -> Values.parseDate(value, "from"))
            .orElseGet(() -> today.minusDays(89));
        var to = request.queryParam("to").map(value -> Values.parseDate(value, "to")).orElse(today);

        var result = queries.dispatch(new GetComplianceStatsQuery(
            from, to, Values.parseFlag(request.queryParam("includeArchived"))));
        if (result.isFailure()) {
            return HttpResponse.error(result.error());
        }

        return HttpResponse.ok(Json.write(RoutineResponses.complianceStats(result.value())));
    }

    private static HttpResponse okOrError(Result<RoutineDto> result) {
        if (result.isFailure()) {
            return HttpResponse.error(result.error());
        }

        return HttpResponse.ok(Json.write(RoutineResponses.routine(result.value())));
    }

    private static HttpResponse noContentOrError(Result<Void> result) {
        if (result.isFailure()) {
            return HttpResponse.error(result.error());
        }

        return HttpResponse.noContent();
    }

    private static RoutineId routineId(HttpRequest request) {
        return RoutineId.parse(request.pathParam("id"));
    }
}
