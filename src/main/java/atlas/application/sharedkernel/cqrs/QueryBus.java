package atlas.application.sharedkernel.cqrs;

public interface QueryBus {

    <R> R dispatch(Query<R> query);
}
