package atlas.domain.presence;

import atlas.domain.sharedkernel.types.PrefixedIds;
import java.util.Optional;

public record AuthenticationGateId(long value) {

    private static final char PREFIX = 'G';
    private static final int NUMERIC_LENGTH = 8;

    public AuthenticationGateId {
        PrefixedIds.format(PREFIX, NUMERIC_LENGTH, value);
    }

    public static AuthenticationGateId single() {
        return new AuthenticationGateId(1);
    }

    public static AuthenticationGateId of(long value) {
        return new AuthenticationGateId(value);
    }

    public static AuthenticationGateId parse(String text) {
        return new AuthenticationGateId(PrefixedIds.parse(PREFIX, NUMERIC_LENGTH, text));
    }

    public static Optional<AuthenticationGateId> tryParse(String text) {
        return PrefixedIds.tryParse(PREFIX, NUMERIC_LENGTH, text).map(AuthenticationGateId::new);
    }

    @Override
    public String toString() {
        return PrefixedIds.format(PREFIX, NUMERIC_LENGTH, value);
    }
}
