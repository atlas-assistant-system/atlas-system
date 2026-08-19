package atlas.application.sharedkernel.ports;

import atlas.domain.sharedkernel.ddd.AggregateRoot;
import java.util.List;
import java.util.Optional;

public interface Repository<T extends AggregateRoot<TId>, TId> {

    Optional<T> get(TId id);

    List<T> getAll();

    void create(T aggregate);

    void update(T aggregate);

    void delete(T aggregate);
}
