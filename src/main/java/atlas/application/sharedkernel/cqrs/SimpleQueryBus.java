package atlas.application.sharedkernel.cqrs;

import java.util.HashMap;
import java.util.Map;

public final class SimpleQueryBus implements QueryBus {

    private final Map<Class<?>, QueryHandler<?, ?>> handlers = new HashMap<>();

    public <Q extends Query<R>, R> void register(Class<Q> queryType, QueryHandler<Q, R> handler) {
        if (handlers.containsKey(queryType)) {
            throw new IllegalStateException("A handler is already registered for " + queryType.getName());
        }

        handlers.put(queryType, handler);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> R dispatch(Query<R> query) {
        var handler = (QueryHandler<Query<R>, R>) handlers.get(query.getClass());

        if (handler == null) {
            throw new IllegalStateException("No handler registered for " + query.getClass().getName());
        }

        return handler.handle(query);
    }
}
