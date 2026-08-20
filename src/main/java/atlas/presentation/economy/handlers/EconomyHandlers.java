package atlas.presentation.economy.handlers;

import atlas.application.economy.commands.abandonsavingsgoal.AbandonSavingsGoalCommand;
import atlas.application.economy.commands.changebudgetlimit.ChangeBudgetLimitCommand;
import atlas.application.economy.commands.changesavingsgoal.ChangeSavingsGoalCommand;
import atlas.application.economy.commands.correctmovement.CorrectMovementCommand;
import atlas.application.economy.commands.definebudget.DefineBudgetCommand;
import atlas.application.economy.commands.deletemovement.DeleteMovementCommand;
import atlas.application.economy.commands.recategorizemovement.RecategorizeMovementCommand;
import atlas.application.economy.commands.recordmovement.RecordMovementCommand;
import atlas.application.economy.commands.removebudget.RemoveBudgetCommand;
import atlas.application.economy.commands.setsavingsgoal.SetSavingsGoalCommand;
import atlas.application.economy.dto.BudgetDto;
import atlas.application.economy.dto.MovementDto;
import atlas.application.economy.dto.SavingsGoalDto;
import atlas.application.economy.queries.getbalance.GetBalanceQuery;
import atlas.application.economy.queries.getbreakdown.GetBreakdownQuery;
import atlas.application.economy.queries.getmovement.GetMovementQuery;
import atlas.application.economy.queries.listbudgets.ListBudgetsQuery;
import atlas.application.economy.queries.listmovements.ListMovementsQuery;
import atlas.application.economy.queries.listsavingsgoals.ListSavingsGoalsQuery;
import atlas.application.sharedkernel.cqrs.CommandBus;
import atlas.application.sharedkernel.cqrs.QueryBus;
import atlas.domain.economy.BudgetId;
import atlas.domain.economy.MovementId;
import atlas.domain.economy.SavingsGoalId;
import atlas.domain.economy.enums.Category;
import atlas.domain.sharedkernel.results.Result;
import atlas.presentation.common.web.Json;
import atlas.presentation.economy.requests.ChangeBudgetLimitRequest;
import atlas.presentation.economy.requests.CorrectMovementRequest;
import atlas.presentation.economy.requests.DefineBudgetRequest;
import atlas.presentation.economy.requests.RecategorizeMovementRequest;
import atlas.presentation.economy.requests.RecordMovementRequest;
import atlas.presentation.economy.requests.SavingsGoalRequest;
import atlas.presentation.economy.responses.EconomyResponses;
import atlas.presentation.economy.web.Values;
import atlas.presentation.sharedkernel.http.HttpRequest;
import atlas.presentation.sharedkernel.http.HttpResponse;
import java.time.LocalDate;

public final class EconomyHandlers {

    private final CommandBus commands;
    private final QueryBus queries;

    public EconomyHandlers(CommandBus commands, QueryBus queries) {
        this.commands = commands;
        this.queries = queries;
    }

    public HttpResponse record(HttpRequest request) {
        var body = RecordMovementRequest.from(Json.parse(request.body()));

        Result<MovementDto> result = commands.dispatch(new RecordMovementCommand(
            body.kind(), body.amount(), body.category(), body.note(), body.occurredOn()));
        if (result.isFailure()) {
            return HttpResponse.error(result.error());
        }

        return HttpResponse.created(
            "/economy/movements/" + result.value().id(),
            Json.write(EconomyResponses.movement(result.value())));
    }

    public HttpResponse correct(HttpRequest request) {
        var body = CorrectMovementRequest.from(Json.parse(request.body()));

        Result<MovementDto> result = commands.dispatch(new CorrectMovementCommand(
            movementId(request), body.amount(), body.note(), body.occurredOn()));

        return okOrError(result);
    }

    public HttpResponse recategorize(HttpRequest request) {
        var body = RecategorizeMovementRequest.from(Json.parse(request.body()));

        Result<MovementDto> result = commands.dispatch(
            new RecategorizeMovementCommand(movementId(request), body.category()));

        return okOrError(result);
    }

    public HttpResponse delete(HttpRequest request) {
        Result<Void> result = commands.dispatch(new DeleteMovementCommand(movementId(request)));
        if (result.isFailure()) {
            return HttpResponse.error(result.error());
        }

        return HttpResponse.noContent();
    }

    public HttpResponse detail(HttpRequest request) {
        Result<MovementDto> result = queries.dispatch(new GetMovementQuery(movementId(request)));

        return okOrError(result);
    }

    public HttpResponse list(HttpRequest request) {
        var result = queries.dispatch(new ListMovementsQuery(
            from(request), to(request), category(request), Values.parseLimit(request.queryParam("limit"))));
        if (result.isFailure()) {
            return HttpResponse.error(result.error());
        }

        return HttpResponse.ok(Json.write(EconomyResponses.movements(result.value())));
    }

    public HttpResponse balance(HttpRequest request) {
        var result = queries.dispatch(new GetBalanceQuery(from(request), to(request)));
        if (result.isFailure()) {
            return HttpResponse.error(result.error());
        }

        return HttpResponse.ok(Json.write(EconomyResponses.balance(result.value())));
    }

    public HttpResponse breakdown(HttpRequest request) {
        var result = queries.dispatch(new GetBreakdownQuery(from(request), to(request)));
        if (result.isFailure()) {
            return HttpResponse.error(result.error());
        }

        return HttpResponse.ok(Json.write(EconomyResponses.breakdown(result.value())));
    }

    public HttpResponse defineBudget(HttpRequest request) {
        var body = DefineBudgetRequest.from(Json.parse(request.body()));

        Result<BudgetDto> result = commands.dispatch(new DefineBudgetCommand(body.category(), body.limit()));
        if (result.isFailure()) {
            return HttpResponse.error(result.error());
        }

        return HttpResponse.created(
            "/economy/budgets/" + result.value().id(), Json.write(EconomyResponses.budget(result.value())));
    }

    public HttpResponse changeBudgetLimit(HttpRequest request) {
        var body = ChangeBudgetLimitRequest.from(Json.parse(request.body()));

        Result<BudgetDto> result = commands.dispatch(
            new ChangeBudgetLimitCommand(budgetId(request), body.limit()));
        if (result.isFailure()) {
            return HttpResponse.error(result.error());
        }

        return HttpResponse.ok(Json.write(EconomyResponses.budget(result.value())));
    }

    public HttpResponse removeBudget(HttpRequest request) {
        Result<Void> result = commands.dispatch(new RemoveBudgetCommand(budgetId(request)));
        if (result.isFailure()) {
            return HttpResponse.error(result.error());
        }

        return HttpResponse.noContent();
    }

    public HttpResponse budgets(HttpRequest request) {
        var result = queries.dispatch(new ListBudgetsQuery());
        if (result.isFailure()) {
            return HttpResponse.error(result.error());
        }

        return HttpResponse.ok(Json.write(EconomyResponses.budgets(result.value())));
    }

    public HttpResponse setGoal(HttpRequest request) {
        var body = SavingsGoalRequest.from(Json.parse(request.body()));

        Result<SavingsGoalDto> result = commands.dispatch(
            new SetSavingsGoalCommand(body.name(), body.target(), body.deadline()));
        if (result.isFailure()) {
            return HttpResponse.error(result.error());
        }

        return HttpResponse.created(
            "/economy/goals/" + result.value().id(), Json.write(EconomyResponses.savingsGoal(result.value())));
    }

    public HttpResponse changeGoal(HttpRequest request) {
        var body = SavingsGoalRequest.from(Json.parse(request.body()));

        Result<SavingsGoalDto> result = commands.dispatch(
            new ChangeSavingsGoalCommand(goalId(request), body.name(), body.target(), body.deadline()));
        if (result.isFailure()) {
            return HttpResponse.error(result.error());
        }

        return HttpResponse.ok(Json.write(EconomyResponses.savingsGoal(result.value())));
    }

    public HttpResponse abandonGoal(HttpRequest request) {
        Result<Void> result = commands.dispatch(new AbandonSavingsGoalCommand(goalId(request)));
        if (result.isFailure()) {
            return HttpResponse.error(result.error());
        }

        return HttpResponse.noContent();
    }

    public HttpResponse goals(HttpRequest request) {
        var result = queries.dispatch(new ListSavingsGoalsQuery());
        if (result.isFailure()) {
            return HttpResponse.error(result.error());
        }

        return HttpResponse.ok(Json.write(EconomyResponses.savingsGoals(result.value())));
    }

    private static SavingsGoalId goalId(HttpRequest request) {
        return SavingsGoalId.parse(request.pathParam("id"));
    }

    private static BudgetId budgetId(HttpRequest request) {
        return BudgetId.parse(request.pathParam("id"));
    }

    private static HttpResponse okOrError(Result<MovementDto> result) {
        if (result.isFailure()) {
            return HttpResponse.error(result.error());
        }

        return HttpResponse.ok(Json.write(EconomyResponses.movement(result.value())));
    }

    private static LocalDate from(HttpRequest request) {
        return request.queryParam("from").map(value -> Values.parseDate(value, "from")).orElse(null);
    }

    private static LocalDate to(HttpRequest request) {
        return request.queryParam("to").map(value -> Values.parseDate(value, "to")).orElse(null);
    }

    private static Category category(HttpRequest request) {
        return request.queryParam("category")
            .map(value -> Values.parseEnum(value, Category.class, "category"))
            .orElse(null);
    }

    private static MovementId movementId(HttpRequest request) {
        return MovementId.parse(request.pathParam("id"));
    }
}
