package atlas.presentation.nutrition.handlers;

import atlas.application.nutrition.commands.adjustplan.AdjustPlanCommand;
import atlas.application.nutrition.commands.archiveplan.ArchivePlanCommand;
import atlas.application.nutrition.commands.correctintake.CorrectIntakeCommand;
import atlas.application.nutrition.commands.defineplan.DefinePlanCommand;
import atlas.application.nutrition.commands.deleteintake.DeleteIntakeCommand;
import atlas.application.nutrition.commands.recordintake.RecordIntakeCommand;
import atlas.application.nutrition.dto.IntakeDto;
import atlas.application.nutrition.dto.PlanDto;
import atlas.application.nutrition.queries.getactiveplan.GetActivePlanQuery;
import atlas.application.nutrition.queries.getday.GetDayQuery;
import atlas.application.nutrition.queries.getintake.GetIntakeQuery;
import atlas.application.nutrition.queries.listdays.ListDaysQuery;
import atlas.application.nutrition.queries.listintakes.ListIntakesQuery;
import atlas.application.sharedkernel.cqrs.CommandBus;
import atlas.application.sharedkernel.cqrs.QueryBus;
import atlas.domain.nutrition.IntakeId;
import atlas.domain.sharedkernel.results.Result;
import atlas.presentation.common.web.Json;
import atlas.presentation.nutrition.requests.AdjustPlanRequest;
import atlas.presentation.nutrition.requests.CorrectIntakeRequest;
import atlas.presentation.nutrition.requests.DefinePlanRequest;
import atlas.presentation.nutrition.requests.RecordIntakeRequest;
import atlas.presentation.nutrition.responses.NutritionResponses;
import atlas.presentation.nutrition.web.Values;
import atlas.presentation.sharedkernel.http.HttpRequest;
import atlas.presentation.sharedkernel.http.HttpResponse;
import java.time.LocalDate;

public final class NutritionHandlers {

    private final CommandBus commands;
    private final QueryBus queries;

    public NutritionHandlers(CommandBus commands, QueryBus queries) {
        this.commands = commands;
        this.queries = queries;
    }

    public HttpResponse definePlan(HttpRequest request) {
        var body = DefinePlanRequest.from(Json.parse(request.body()));

        Result<PlanDto> result = commands.dispatch(new DefinePlanCommand(
            body.startWeight(), body.targetWeight(), body.calories(), body.protein(), body.carbs(),
            body.fat(), body.startedOn()));
        if (result.isFailure()) {
            return HttpResponse.error(result.error());
        }

        return HttpResponse.created("/nutrition/plan", Json.write(NutritionResponses.plan(result.value())));
    }

    public HttpResponse adjustPlan(HttpRequest request) {
        var body = AdjustPlanRequest.from(Json.parse(request.body()));

        Result<PlanDto> result = commands.dispatch(
            new AdjustPlanCommand(
                body.targetWeight(), body.calories(), body.protein(), body.carbs(), body.fat()));

        return planOrError(result);
    }

    public HttpResponse archivePlan(HttpRequest request) {
        Result<Void> result = commands.dispatch(new ArchivePlanCommand());
        if (result.isFailure()) {
            return HttpResponse.error(result.error());
        }

        return HttpResponse.noContent();
    }

    public HttpResponse activePlan(HttpRequest request) {
        Result<PlanDto> result = queries.dispatch(new GetActivePlanQuery());

        return planOrError(result);
    }

    public HttpResponse recordIntake(HttpRequest request) {
        var body = RecordIntakeRequest.from(Json.parse(request.body()));

        Result<IntakeDto> result = commands.dispatch(new RecordIntakeCommand(
            body.calories(), body.protein(), body.carbs(), body.fat(), body.note(),
            body.consumedOn()));
        if (result.isFailure()) {
            return HttpResponse.error(result.error());
        }

        return HttpResponse.created(
            "/nutrition/intakes/" + result.value().id(),
            Json.write(NutritionResponses.intake(result.value())));
    }

    public HttpResponse correctIntake(HttpRequest request) {
        var body = CorrectIntakeRequest.from(Json.parse(request.body()));

        Result<IntakeDto> result = commands.dispatch(new CorrectIntakeCommand(
            intakeId(request), body.calories(), body.protein(), body.carbs(), body.fat(),
            body.note()));

        return intakeOrError(result);
    }

    public HttpResponse deleteIntake(HttpRequest request) {
        Result<Void> result = commands.dispatch(new DeleteIntakeCommand(intakeId(request)));
        if (result.isFailure()) {
            return HttpResponse.error(result.error());
        }

        return HttpResponse.noContent();
    }

    public HttpResponse intakeDetail(HttpRequest request) {
        Result<IntakeDto> result = queries.dispatch(new GetIntakeQuery(intakeId(request)));

        return intakeOrError(result);
    }

    public HttpResponse listIntakes(HttpRequest request) {
        var result = queries.dispatch(new ListIntakesQuery(
            from(request), to(request), Values.parseLimit(request.queryParam("limit"))));
        if (result.isFailure()) {
            return HttpResponse.error(result.error());
        }

        return HttpResponse.ok(Json.write(NutritionResponses.intakes(result.value())));
    }

    public HttpResponse day(HttpRequest request) {
        var result = queries.dispatch(
            new GetDayQuery(Values.parseDate(request.pathParam("date"), "date")));
        if (result.isFailure()) {
            return HttpResponse.error(result.error());
        }

        return HttpResponse.ok(Json.write(NutritionResponses.day(result.value())));
    }

    public HttpResponse today(HttpRequest request) {
        var result = queries.dispatch(new GetDayQuery(null));
        if (result.isFailure()) {
            return HttpResponse.error(result.error());
        }

        return HttpResponse.ok(Json.write(NutritionResponses.day(result.value())));
    }

    public HttpResponse days(HttpRequest request) {
        var result = queries.dispatch(new ListDaysQuery(from(request), to(request)));
        if (result.isFailure()) {
            return HttpResponse.error(result.error());
        }

        return HttpResponse.ok(Json.write(NutritionResponses.days(result.value())));
    }

    private static HttpResponse planOrError(Result<PlanDto> result) {
        if (result.isFailure()) {
            return HttpResponse.error(result.error());
        }

        return HttpResponse.ok(Json.write(NutritionResponses.plan(result.value())));
    }

    private static HttpResponse intakeOrError(Result<IntakeDto> result) {
        if (result.isFailure()) {
            return HttpResponse.error(result.error());
        }

        return HttpResponse.ok(Json.write(NutritionResponses.intake(result.value())));
    }

    private static LocalDate from(HttpRequest request) {
        return request.queryParam("from").map(value -> Values.parseDate(value, "from")).orElse(null);
    }

    private static LocalDate to(HttpRequest request) {
        return request.queryParam("to").map(value -> Values.parseDate(value, "to")).orElse(null);
    }

    private static IntakeId intakeId(HttpRequest request) {
        return IntakeId.parse(request.pathParam("id"));
    }
}
