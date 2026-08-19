package sharedkernel.domain.results;

public enum ErrorType {
    NONE,
    VALIDATION,
    NOT_FOUND,
    CONFLICT,
    UNAUTHORIZED,
    FORBIDDEN,
    FAILURE,
    UNEXPECTED
}
