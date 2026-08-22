package atlas.presentation.training.sse;

import atlas.application.sharedkernel.events.SimpleDomainEventPublisher;
import atlas.domain.training.events.ExerciseArchivedEvent;
import atlas.domain.training.events.ExerciseDefinedEvent;
import atlas.domain.training.events.ExerciseRenamedEvent;
import atlas.domain.training.events.ExerciseUnarchivedEvent;
import atlas.domain.training.events.SetRecordedEvent;
import atlas.domain.training.events.SetRemovedEvent;
import atlas.domain.training.events.WorkoutArchivedEvent;
import atlas.domain.training.events.WorkoutDefinedEvent;
import atlas.domain.training.events.WorkoutLogDiscardedEvent;
import atlas.domain.training.events.WorkoutPlanChangedEvent;
import atlas.domain.training.events.WorkoutRenamedEvent;
import atlas.domain.training.events.WorkoutStartedEvent;
import atlas.presentation.common.web.Json;
import atlas.presentation.sharedkernel.sse.SseEvent;
import atlas.presentation.sharedkernel.sse.SseHub;
import java.util.Map;

public final class TrainingEventsBroadcaster {

    private TrainingEventsBroadcaster() {}

    public static void subscribeAll(SimpleDomainEventPublisher events, SseHub hub) {
        events.subscribe(ExerciseDefinedEvent.class,
            event -> broadcast(hub, "exerciseDefined", "exerciseId", event.exerciseId().toString()));
        events.subscribe(ExerciseRenamedEvent.class,
            event -> broadcast(hub, "exerciseRenamed", "exerciseId", event.exerciseId().toString()));
        events.subscribe(ExerciseArchivedEvent.class,
            event -> broadcast(hub, "exerciseArchived", "exerciseId", event.exerciseId().toString()));
        events.subscribe(ExerciseUnarchivedEvent.class,
            event -> broadcast(hub, "exerciseUnarchived", "exerciseId", event.exerciseId().toString()));

        events.subscribe(WorkoutDefinedEvent.class,
            event -> broadcast(hub, "workoutDefined", "workoutId", event.workoutId().toString()));
        events.subscribe(WorkoutRenamedEvent.class,
            event -> broadcast(hub, "workoutRenamed", "workoutId", event.workoutId().toString()));
        events.subscribe(WorkoutPlanChangedEvent.class,
            event -> broadcast(hub, "workoutPlanChanged", "workoutId", event.workoutId().toString()));
        events.subscribe(WorkoutArchivedEvent.class,
            event -> broadcast(hub, "workoutArchived", "workoutId", event.workoutId().toString()));

        events.subscribe(WorkoutStartedEvent.class,
            event -> broadcast(hub, "workoutStarted", "logId", event.workoutLogId().toString()));
        events.subscribe(SetRecordedEvent.class,
            event -> broadcast(hub, "setRecorded", "logId", event.workoutLogId().toString()));
        events.subscribe(SetRemovedEvent.class,
            event -> broadcast(hub, "setRemoved", "logId", event.workoutLogId().toString()));
        events.subscribe(WorkoutLogDiscardedEvent.class,
            event -> broadcast(hub, "workoutDiscarded", "logId", event.workoutLogId().toString()));
    }

    private static void broadcast(SseHub hub, String name, String field, String id) {
        hub.broadcast(SseEvent.named(name, Json.write(Map.of(field, id))));
    }
}
