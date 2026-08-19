package sharedkernel.application.logging;

import java.lang.System.Logger.Level;
import java.time.Clock;
import sharedkernel.application.cqrs.Query;
import sharedkernel.application.cqrs.QueryHandler;
import sharedkernel.domain.guards.ObjectGuard;

public final class LoggingQueryHandler<Q extends Query<R>, R> implements QueryHandler<Q, R> {

    private static final System.Logger DEFAULT_LOG = System.getLogger("sharedkernel.query");

    private final QueryHandler<Q, R> inner;
    private final System.Logger log;
    private final LogEntryRenderer renderer;
    private final Clock clock;

    public LoggingQueryHandler(QueryHandler<Q, R> inner) {
        this(inner, DEFAULT_LOG, new PlainLogEntryRenderer(), Clock.systemDefaultZone());
    }

    public LoggingQueryHandler(QueryHandler<Q, R> inner, LogEntryRenderer renderer) {
        this(inner, DEFAULT_LOG, renderer, Clock.systemDefaultZone());
    }

    public LoggingQueryHandler(QueryHandler<Q, R> inner, System.Logger log, LogEntryRenderer renderer, Clock clock) {
        this.inner = ObjectGuard.notNull(inner, "inner");
        this.log = ObjectGuard.notNull(log, "log");
        this.renderer = ObjectGuard.notNull(renderer, "renderer");
        this.clock = ObjectGuard.notNull(clock, "clock");
    }

    @Override
    public R handle(Q query) {
        ObjectGuard.notNull(query, "query");

        long startedAt = System.nanoTime();

        try {
            R result = inner.handle(query);
            var entry = HandlerLogging.entryFor(
                query, LogKind.QUERY, result, null, HandlerLogging.elapsedMs(startedAt), clock);
            HandlerLogging.write(log, renderer, entry, null, Level.DEBUG);

            return result;
        } catch (RuntimeException e) {
            var entry = HandlerLogging.entryFor(
                query, LogKind.QUERY, null, e, HandlerLogging.elapsedMs(startedAt), clock);
            HandlerLogging.write(log, renderer, entry, e, Level.DEBUG);

            throw e;
        }
    }
}
