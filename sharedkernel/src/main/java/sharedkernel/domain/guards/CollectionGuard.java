package sharedkernel.domain.guards;

import java.util.Collection;
import sharedkernel.domain.exceptions.GuardException;

public final class CollectionGuard {

    private CollectionGuard() {}

    public static <T, C extends Collection<T>> C notEmpty(C value, String parameterName) {
        if (value == null || value.isEmpty()) {
            throw GuardException.forParameter(parameterName, "cannot be empty");
        }

        return value;
    }

    public static <T, C extends Collection<T>> C noNullElements(C value, String parameterName) {
        notEmpty(value, parameterName);

        for (var element : value) {
            if (element == null) {
                throw GuardException.forParameter(parameterName, "cannot contain null elements");
            }
        }

        return value;
    }
}
