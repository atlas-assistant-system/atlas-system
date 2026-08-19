package sharedkernel.application.outbox;

import java.time.Instant;
import java.util.UUID;

public record OutboxMessage(
    String id,
    String type,
    String payload,
    Instant occurredOn,
    Instant processedAt,
    String error,
    int retryCount) {

    public static OutboxMessage pending(String type, String payload, Instant occurredOn) {
        return new OutboxMessage(UUID.randomUUID().toString(), type, payload, occurredOn, null, null, 0);
    }

    public boolean isPending() {
        return processedAt == null;
    }
}
