package sharedkernel.application.logging;

import java.lang.System.Logger.Level;
import java.time.Clock;
import sharedkernel.application.cqrs.Command;
import sharedkernel.application.cqrs.CommandHandler;
import sharedkernel.domain.guards.ObjectGuard;

public final class LoggingCommandHandler<C extends Command<R>, R> implements CommandHandler<C, R> {

    private static final System.Logger DEFAULT_LOG = System.getLogger("sharedkernel.command");

    private final CommandHandler<C, R> inner;
    private final System.Logger log;
    private final LogEntryRenderer renderer;
    private final Clock clock;

    public LoggingCommandHandler(CommandHandler<C, R> inner) {
        this(inner, DEFAULT_LOG, new PlainLogEntryRenderer(), Clock.systemDefaultZone());
    }

    public LoggingCommandHandler(CommandHandler<C, R> inner, LogEntryRenderer renderer) {
        this(inner, DEFAULT_LOG, renderer, Clock.systemDefaultZone());
    }

    public LoggingCommandHandler(
        CommandHandler<C, R> inner, System.Logger log, LogEntryRenderer renderer, Clock clock) {
        this.inner = ObjectGuard.notNull(inner, "inner");
        this.log = ObjectGuard.notNull(log, "log");
        this.renderer = ObjectGuard.notNull(renderer, "renderer");
        this.clock = ObjectGuard.notNull(clock, "clock");
    }

    @Override
    public R handle(C command) {
        ObjectGuard.notNull(command, "command");

        long startedAt = System.nanoTime();

        try {
            R result = inner.handle(command);
            var entry = HandlerLogging.entryFor(
                command, LogKind.COMMAND, result, null, HandlerLogging.elapsedMs(startedAt), clock);
            HandlerLogging.write(log, renderer, entry, null, Level.INFO);

            return result;
        } catch (RuntimeException e) {
            var entry = HandlerLogging.entryFor(
                command, LogKind.COMMAND, null, e, HandlerLogging.elapsedMs(startedAt), clock);
            HandlerLogging.write(log, renderer, entry, e, Level.INFO);

            throw e;
        }
    }
}
