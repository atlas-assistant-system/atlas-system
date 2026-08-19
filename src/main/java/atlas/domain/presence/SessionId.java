package atlas.domain.presence;

import java.util.Optional;
import sharedkernel.domain.types.PrefixedIds;

public record SessionId(long value) {

    private static final char PREFIX = 'S';
    private static final int NUMERIC_LENGTH = 8;

    public SessionId {
        PrefixedIds.format(PREFIX, NUMERIC_LENGTH, value);
    }

    public static SessionId of(long value) {
        return new SessionId(value);
    }

    public static SessionId parse(String text) {
        return new SessionId(PrefixedIds.parse(PREFIX, NUMERIC_LENGTH, text));
    }

    public static Optional<SessionId> tryParse(String text) {
        return PrefixedIds.tryParse(PREFIX, NUMERIC_LENGTH, text).map(SessionId::new);
    }

    @Override
    public String toString() {
        return PrefixedIds.format(PREFIX, NUMERIC_LENGTH, value);
    }
}
