package atlas.domain.sharedkernel.guards;

import atlas.domain.sharedkernel.exceptions.GuardException;

public final class NumberGuard {

    private NumberGuard() {}

    public static long notNegative(long value, String parameterName) {
        if (value < 0) {
            throw GuardException.forParameter(parameterName, "cannot be negative");
        }

        return value;
    }

    public static int notNegative(int value, String parameterName) {
        if (value < 0) {
            throw GuardException.forParameter(parameterName, "cannot be negative");
        }

        return value;
    }

    public static long positive(long value, String parameterName) {
        if (value <= 0) {
            throw GuardException.forParameter(parameterName, "must be greater than zero");
        }

        return value;
    }

    public static long inRange(long value, long minimum, long maximum, String parameterName) {
        if (value < minimum || value > maximum) {
            throw GuardException.forParameter(parameterName, "must be between " + minimum + " and " + maximum);
        }

        return value;
    }
}
