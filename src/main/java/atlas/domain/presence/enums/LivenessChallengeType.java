package atlas.domain.presence.enums;

import java.time.Duration;

public enum LivenessChallengeType {

    FIST(Duration.ofSeconds(10));

    private final Duration timeout;

    LivenessChallengeType(Duration timeout) {
        this.timeout = timeout;
    }

    public Duration timeout() {
        return timeout;
    }
}
