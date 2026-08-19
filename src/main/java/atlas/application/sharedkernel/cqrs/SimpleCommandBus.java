package atlas.application.sharedkernel.cqrs;

import java.util.HashMap;
import java.util.Map;

public final class SimpleCommandBus implements CommandBus {

    private final Map<Class<?>, CommandHandler<?, ?>> handlers = new HashMap<>();

    public <C extends Command<R>, R> void register(Class<C> commandType, CommandHandler<C, R> handler) {
        if (handlers.containsKey(commandType)) {
            throw new IllegalStateException("A handler is already registered for " + commandType.getName());
        }

        handlers.put(commandType, handler);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> R dispatch(Command<R> command) {
        var handler = (CommandHandler<Command<R>, R>) handlers.get(command.getClass());

        if (handler == null) {
            throw new IllegalStateException("No handler registered for " + command.getClass().getName());
        }

        return handler.handle(command);
    }
}
