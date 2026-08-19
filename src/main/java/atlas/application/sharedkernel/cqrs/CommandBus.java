package atlas.application.sharedkernel.cqrs;

public interface CommandBus {

    <R> R dispatch(Command<R> command);
}
