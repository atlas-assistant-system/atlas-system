package atlas.domain.sharedkernel.guards;

import atlas.domain.sharedkernel.exceptions.GuardException;
import java.util.regex.Pattern;

public final class StringGuard {

    private StringGuard() {}

    public static String notBlank(String value, String parameterName) {
        if (value == null || value.isBlank()) {
            throw GuardException.forParameter(parameterName, "cannot be blank");
        }

        return value;
    }

    public static String notLongerThan(String value, int maxLength, String parameterName) {
        notBlank(value, parameterName);

        if (value.length() > maxLength) {
            throw GuardException.forParameter(parameterName, "cannot exceed " + maxLength + " characters");
        }

        return value;
    }

    public static String matches(String value, Pattern pattern, String parameterName) {
        notBlank(value, parameterName);

        if (!pattern.matcher(value).matches()) {
            throw GuardException.forParameter(parameterName, "does not match " + pattern.pattern());
        }

        return value;
    }
}
