package sharedkernel.application.outbox;

import java.time.Instant;
import java.util.List;

public interface OutboxStore {

    void append(List<OutboxMessage> messages);

    List<OutboxMessage> pending(int batchSize, int maxRetryCount);

    void markProcessed(String id, Instant processedAt);

    void markFailed(String id, String error);
}
