package sharedkernel.application.cqrs;

public interface QueryBus {

    <R> R dispatch(Query<R> query);
}
