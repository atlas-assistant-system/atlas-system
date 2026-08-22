package atlas.presentation.training.handlers;

import atlas.application.sharedkernel.cqrs.CommandBus;
import atlas.application.sharedkernel.cqrs.QueryBus;
import atlas.application.training.commands.addset.AddSetCommand;
import atlas.application.training.commands.archiveexercise.ArchiveExerciseCommand;
import atlas.application.training.commands.archiveworkout.ArchiveWorkoutCommand;
import atlas.application.training.commands.defineexercise.DefineExerciseCommand;
import atlas.application.training.commands.defineworkout.DefineWorkoutCommand;
import atlas.application.training.commands.discardworkoutlog.DiscardWorkoutLogCommand;
import atlas.application.training.commands.recordset.RecordSetCommand;
import atlas.application.training.commands.removeset.RemoveSetCommand;
import atlas.application.training.commands.renameexercise.RenameExerciseCommand;
import atlas.application.training.commands.renameworkout.RenameWorkoutCommand;
import atlas.application.training.commands.scheduleworkout.ScheduleWorkoutCommand;
import atlas.application.training.commands.setworkoutplan.SetWorkoutPlanCommand;
import atlas.application.training.commands.startworkoutlog.StartWorkoutLogCommand;
import atlas.application.training.commands.unarchiveexercise.UnarchiveExerciseCommand;
import atlas.application.training.dto.ExerciseDto;
import atlas.application.training.dto.WorkoutDto;
import atlas.application.training.dto.WorkoutLogDto;
import atlas.application.training.queries.gettodayworkout.GetTodayWorkoutQuery;
import atlas.application.training.queries.getworkout.GetWorkoutQuery;
import atlas.application.training.queries.getworkoutlog.GetWorkoutLogQuery;
import atlas.application.training.queries.listexercises.ListExercisesQuery;
import atlas.application.training.queries.listworkoutlogs.ListWorkoutLogsQuery;
import atlas.application.training.queries.listworkouts.ListWorkoutsQuery;
import atlas.domain.sharedkernel.exceptions.FormatException;
import atlas.domain.sharedkernel.results.Result;
import atlas.domain.training.ExerciseId;
import atlas.domain.training.WorkoutId;
import atlas.domain.training.WorkoutLogId;
import atlas.domain.training.entities.SetLogId;
import atlas.presentation.common.web.Json;
import atlas.presentation.sharedkernel.http.HttpRequest;
import atlas.presentation.sharedkernel.http.HttpResponse;
import atlas.presentation.training.requests.TrainingRequests;
import atlas.presentation.training.responses.TrainingResponses;
import atlas.presentation.training.web.Values;
import java.time.LocalDate;
import java.util.UUID;

public final class TrainingHandlers {

    private final CommandBus commands;
    private final QueryBus queries;

    public TrainingHandlers(CommandBus commands, QueryBus queries) {
        this.commands = commands;
        this.queries = queries;
    }

    public HttpResponse defineExercise(HttpRequest request) {
        var body = Json.parse(request.body());

        Result<ExerciseDto> result = commands.dispatch(new DefineExerciseCommand(
            Values.text(body, "name"), Values.metric(body, "metric")));
        if (result.isFailure()) {
            return HttpResponse.error(result.error());
        }

        return HttpResponse.created(
            "/training/exercises/" + result.value().id(),
            Json.write(TrainingResponses.exercise(result.value())));
    }

    public HttpResponse listExercises(HttpRequest request) {
        var result = queries.dispatch(
            new ListExercisesQuery(Values.flag(request.queryParam("archived"))));

        return result.isFailure()
            ? HttpResponse.error(result.error())
            : HttpResponse.ok(Json.write(TrainingResponses.exercises(result.value())));
    }

    public HttpResponse renameExercise(HttpRequest request) {
        var body = Json.parse(request.body());

        Result<ExerciseDto> result = commands.dispatch(
            new RenameExerciseCommand(exerciseId(request), Values.text(body, "name")));

        return result.isFailure()
            ? HttpResponse.error(result.error())
            : HttpResponse.ok(Json.write(TrainingResponses.exercise(result.value())));
    }

    public HttpResponse archiveExercise(HttpRequest request) {
        Result<Void> result = commands.dispatch(new ArchiveExerciseCommand(exerciseId(request)));

        return result.isFailure() ? HttpResponse.error(result.error()) : HttpResponse.noContent();
    }

    public HttpResponse restoreExercise(HttpRequest request) {
        Result<Void> result = commands.dispatch(new UnarchiveExerciseCommand(exerciseId(request)));

        return result.isFailure() ? HttpResponse.error(result.error()) : HttpResponse.noContent();
    }

    public HttpResponse defineWorkout(HttpRequest request) {
        var body = Json.parse(request.body());

        Result<WorkoutDto> result = commands.dispatch(
            new DefineWorkoutCommand(Values.text(body, "name")));
        if (result.isFailure()) {
            return HttpResponse.error(result.error());
        }

        return HttpResponse.created(
            "/training/workouts/" + result.value().id(),
            Json.write(TrainingResponses.workout(result.value())));
    }

    public HttpResponse listWorkouts(HttpRequest request) {
        var result = queries.dispatch(
            new ListWorkoutsQuery(Values.flag(request.queryParam("archived"))));

        return result.isFailure()
            ? HttpResponse.error(result.error())
            : HttpResponse.ok(Json.write(TrainingResponses.workouts(result.value())));
    }

    public HttpResponse workoutDetail(HttpRequest request) {
        Result<WorkoutDto> result = queries.dispatch(new GetWorkoutQuery(workoutId(request)));

        return workoutOrError(result);
    }

    public HttpResponse renameWorkout(HttpRequest request) {
        var body = Json.parse(request.body());

        Result<WorkoutDto> result = commands.dispatch(
            new RenameWorkoutCommand(workoutId(request), Values.text(body, "name")));

        return workoutOrError(result);
    }

    public HttpResponse setWorkoutPlan(HttpRequest request) {
        var body = Json.parse(request.body());

        Result<WorkoutDto> result = commands.dispatch(
            new SetWorkoutPlanCommand(workoutId(request), TrainingRequests.plan(body)));

        return workoutOrError(result);
    }

    public HttpResponse scheduleWorkout(HttpRequest request) {
        var body = Json.parse(request.body());

        Result<WorkoutDto> result = commands.dispatch(
            new ScheduleWorkoutCommand(workoutId(request), Values.weekdays(body, "days")));

        return workoutOrError(result);
    }

    public HttpResponse archiveWorkout(HttpRequest request) {
        Result<Void> result = commands.dispatch(new ArchiveWorkoutCommand(workoutId(request)));

        return result.isFailure() ? HttpResponse.error(result.error()) : HttpResponse.noContent();
    }

    public HttpResponse startWorkoutLog(HttpRequest request) {
        var body = Json.parse(request.body());
        var workout = Values.text(body, "workoutId");

        Result<WorkoutLogDto> result = commands.dispatch(new StartWorkoutLogCommand(
            workout == null ? null : WorkoutId.parse(workout),
            Values.date(body, "performedOn")));
        if (result.isFailure()) {
            return HttpResponse.error(result.error());
        }

        return HttpResponse.created(
            "/training/logs/" + result.value().id(),
            Json.write(TrainingResponses.log(result.value())));
    }

    public HttpResponse listWorkoutLogs(HttpRequest request) {
        var result = queries.dispatch(new ListWorkoutLogsQuery(
            from(request), to(request), Values.parseLimit(request.queryParam("limit"))));

        return result.isFailure()
            ? HttpResponse.error(result.error())
            : HttpResponse.ok(Json.write(TrainingResponses.logs(result.value())));
    }

    public HttpResponse workoutLogDetail(HttpRequest request) {
        Result<WorkoutLogDto> result = queries.dispatch(new GetWorkoutLogQuery(logId(request)));

        return logOrError(result);
    }

    public HttpResponse today(HttpRequest request) {
        var result = queries.dispatch(new GetTodayWorkoutQuery());

        return result.isFailure()
            ? HttpResponse.error(result.error())
            : HttpResponse.ok(Json.write(TrainingResponses.logs(result.value())));
    }

    public HttpResponse discardWorkoutLog(HttpRequest request) {
        Result<Void> result = commands.dispatch(new DiscardWorkoutLogCommand(logId(request)));

        return result.isFailure() ? HttpResponse.error(result.error()) : HttpResponse.noContent();
    }

    public HttpResponse addSet(HttpRequest request) {
        var body = Json.parse(request.body());
        var exercise = Values.text(body, "exerciseId");
        if (exercise == null) {
            throw new FormatException("'exerciseId' is required.");
        }

        Result<WorkoutLogDto> result = commands.dispatch(new AddSetCommand(
            logId(request), ExerciseId.parse(exercise), TrainingRequests.effort(body)));

        return logOrError(result);
    }

    public HttpResponse recordSet(HttpRequest request) {
        var body = Json.parse(request.body());

        Result<WorkoutLogDto> result = commands.dispatch(new RecordSetCommand(
            logId(request), setId(request), TrainingRequests.effort(body)));

        return logOrError(result);
    }

    public HttpResponse removeSet(HttpRequest request) {
        Result<Void> result = commands.dispatch(
            new RemoveSetCommand(logId(request), setId(request)));

        return result.isFailure() ? HttpResponse.error(result.error()) : HttpResponse.noContent();
    }

    private static HttpResponse workoutOrError(Result<WorkoutDto> result) {
        return result.isFailure()
            ? HttpResponse.error(result.error())
            : HttpResponse.ok(Json.write(TrainingResponses.workout(result.value())));
    }

    private static HttpResponse logOrError(Result<WorkoutLogDto> result) {
        return result.isFailure()
            ? HttpResponse.error(result.error())
            : HttpResponse.ok(Json.write(TrainingResponses.log(result.value())));
    }

    private static ExerciseId exerciseId(HttpRequest request) {
        return ExerciseId.parse(request.pathParam("id"));
    }

    private static WorkoutId workoutId(HttpRequest request) {
        return WorkoutId.parse(request.pathParam("id"));
    }

    private static WorkoutLogId logId(HttpRequest request) {
        return WorkoutLogId.parse(request.pathParam("id"));
    }

    private static SetLogId setId(HttpRequest request) {
        try {
            return SetLogId.of(UUID.fromString(request.pathParam("setId")));
        } catch (IllegalArgumentException e) {
            throw new FormatException("'setId' must be a UUID.");
        }
    }

    private static LocalDate from(HttpRequest request) {
        return request.queryParam("from").map(value -> Values.parseDate(value, "from")).orElse(null);
    }

    private static LocalDate to(HttpRequest request) {
        return request.queryParam("to").map(value -> Values.parseDate(value, "to")).orElse(null);
    }
}
