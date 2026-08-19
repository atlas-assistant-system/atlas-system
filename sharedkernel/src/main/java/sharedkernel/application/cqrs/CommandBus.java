package sharedkernel.application.cqrs;

public interface CommandBus {

    <R> R dispatch(Command<R> command);
}
