package atlas.domain.sharedkernel.guards;

import atlas.domain.sharedkernel.exceptions.GuardException;

public final class ObjectGuard {

    private ObjectGuard() {}

    public static <T> T notNull(T value, String parameterName) {
        if (value == null) {
            throw GuardException.forParameter(parameterName, "cannot be null");
        }

        return value;
    }
}
