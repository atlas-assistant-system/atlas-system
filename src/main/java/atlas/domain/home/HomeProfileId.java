package atlas.domain.home;

import atlas.domain.sharedkernel.guards.StringGuard;

public record HomeProfileId(String value) {

    private static final int MAX_LENGTH = 64;

    public HomeProfileId {
        value = StringGuard.notLongerThan(value, MAX_LENGTH, "value").trim();
    }

    public static HomeProfileId of(String value) {
        return new HomeProfileId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
