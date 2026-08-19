package atlas.presentation.appointments.sse;

import atlas.application.appointments.dto.DueReminderDto;
import atlas.application.appointments.queries.getduereminders.GetDueRemindersQuery;
import atlas.application.sharedkernel.cqrs.QueryBus;
import atlas.domain.sharedkernel.results.Result;
import atlas.presentation.appointments.responses.AppointmentResponses;
import atlas.presentation.common.web.Json;
import atlas.presentation.sharedkernel.sse.SseEvent;
import atlas.presentation.sharedkernel.sse.SseHub;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class DueReminderPusher implements AutoCloseable {

    public static final Duration POLL_EVERY = Duration.ofSeconds(30);
    public static final Duration HEARTBEAT_EVERY = Duration.ofSeconds(25);

    private final QueryBus queries;
    private final SseHub hub;
    private final Clock clock;
    private final Set<String> alreadyPushed = new HashSet<>();

    private ScheduledExecutorService scheduler;

    public DueReminderPusher(QueryBus queries, SseHub hub, Clock clock) {
        this.queries = queries;
        this.hub = hub;
        this.clock = clock;
    }

    public void start() {
        scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            var thread = new Thread(runnable, "due-reminder-pusher");
            thread.setDaemon(true);

            return thread;
        });
        scheduler.scheduleAtFixedRate(this::pushDueReminders, 0, POLL_EVERY.toSeconds(), TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(hub::sendHeartbeat, HEARTBEAT_EVERY.toSeconds(), HEARTBEAT_EVERY.toSeconds(),
            TimeUnit.SECONDS);
    }

    void pushDueReminders() {
        Result<List<DueReminderDto>> result = queries.dispatch(new GetDueRemindersQuery(LocalDateTime.now(clock)));
        if (result.isFailure()) {
            return;
        }

        for (var reminder : result.value()) {
            if (alreadyPushed.add(reminder.reminderId())) {
                hub.broadcast(SseEvent.named("reminderDue", Json.write(AppointmentResponses.dueReminder(reminder))));
            }
        }
    }

    @Override
    public void close() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }
}
