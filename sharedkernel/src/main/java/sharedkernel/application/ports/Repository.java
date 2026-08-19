package sharedkernel.application.ports;

import java.util.List;
import java.util.Optional;
import sharedkernel.domain.ddd.AggregateRoot;

public interface Repository<T extends AggregateRoot<TId>, TId> {

    Optional<T> get(TId id);

    List<T> getAll();

    void create(T aggregate);

    void update(T aggregate);

    void delete(T aggregate);
}
