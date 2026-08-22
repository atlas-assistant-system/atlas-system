package atlas.application.sharedkernel.unitofwork;

import atlas.application.sharedkernel.events.PendingEventDispatcher;
import atlas.application.sharedkernel.events.SimpleDomainEventPublisher;
import java.util.List;

final class RecordingUnitOfWork extends AbstractUnitOfWork {

    private final List<String> journal;
    private final boolean failOnCommit;

    RecordingUnitOfWork(List<String> journal, SimpleDomainEventPublisher publisher, boolean failOnCommit) {
        super(new PendingEventDispatcher(publisher));
        this.journal = journal;
        this.failOnCommit = failOnCommit;
    }

    @Override
    protected void begin() {
        journal.add("begin");
    }

    @Override
    protected void commit() {
        if (failOnCommit) {
            throw new IllegalStateException("database is locked");
        }

        journal.add("commit");
    }

    @Override
    protected void rollback() {
        journal.add("rollback");
    }
}
