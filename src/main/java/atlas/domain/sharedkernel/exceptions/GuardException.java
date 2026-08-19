package atlas.domain.sharedkernel.exceptions;

public final class GuardException extends DomainException {

    private final String parameterName;

    private GuardException(String parameterName, String message) {
        super(message);
        this.parameterName = parameterName;
    }

    public static GuardException forParameter(String parameterName, String violation) {
        return new GuardException(parameterName, "'" + parameterName + "' " + violation + ".");
    }

    public String parameterName() {
        return parameterName;
    }
}
