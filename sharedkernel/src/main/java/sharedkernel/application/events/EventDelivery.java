package sharedkernel.application.events;

import java.util.List;
import sharedkernel.domain.ddd.AggregateRoot;

public interface EventDelivery {

    void beforeCommit(List<? extends AggregateRoot<?>> changed);

    void afterCommit(List<? extends AggregateRoot<?>> changed);
}
