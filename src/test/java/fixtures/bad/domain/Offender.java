package fixtures.bad.domain;

import fixtures.bad.application.SomeUseCase;
import java.time.Instant;
import java.util.UUID;

public final class Offender {

    private static final System.Logger LOG = System.getLogger("domain");

    public void logSomething() {
        LOG.log(System.Logger.Level.INFO, "should not happen here");
    }

    public Instant readClock() {
        return Instant.now();
    }

    public UUID rollDice() {
        return UUID.randomUUID();
    }

    public SomeUseCase reachOutwards() {
        return new SomeUseCase();
    }
}
