package atlas.application.sharedkernel.events;

import atlas.domain.sharedkernel.ddd.AggregateRoot;
import java.util.List;

public interface EventDelivery {

    void beforeCommit(List<? extends AggregateRoot<?>> changed);

    void afterCommit(List<? extends AggregateRoot<?>> changed);
}
