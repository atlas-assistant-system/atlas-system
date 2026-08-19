package atlas.presentation.presence.sse;

import atlas.application.presence.commands.expirestalesessions.ExpireStaleSessionsCommand;
import atlas.application.presence.ports.LivenessChallengeRepository;
import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import sharedkernel.application.cqrs.CommandBus;
import sharedkernel.presentation.sse.SseHub;

public final class PresencePoller implements AutoCloseable {

    public static final Duration POLL_EVERY = Duration.ofSeconds(5);
    public static final Duration HEARTBEAT_EVERY = Duration.ofSeconds(25);

    private static final System.Logger LOG = System.getLogger("presence.poller");

    private final CommandBus commands;
    private final LivenessChallengeRepository challenges;
    private final SseHub hub;
    private final Clock clock;

    private ScheduledExecutorService scheduler;

    public PresencePoller(
        CommandBus commands, LivenessChallengeRepository challenges, SseHub hub, Clock clock) {
        this.commands = commands;
        this.challenges = challenges;
        this.hub = hub;
        this.clock = clock;
    }

    public void start() {
        scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            var thread = new Thread(runnable, "presence-poller");
            thread.setDaemon(true);
            return thread;
        });
        scheduler.scheduleAtFixedRate(this::pollSafely, 0, POLL_EVERY.toSeconds(), TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(
            hub::sendHeartbeat,
            HEARTBEAT_EVERY.toSeconds(),
            HEARTBEAT_EVERY.toSeconds(),
            TimeUnit.SECONDS);
    }

    void poll() {
        commands.dispatch(new ExpireStaleSessionsCommand());
        challenges.removeExpired(clock.instant());
    }

    private void pollSafely() {
        try {
            poll();
        } catch (RuntimeException e) {
            LOG.log(System.Logger.Level.ERROR, "Presence polling failed", e);
        }
    }

    @Override
    public void close() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }
}
