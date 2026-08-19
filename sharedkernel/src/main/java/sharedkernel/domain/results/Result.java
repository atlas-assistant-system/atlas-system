package sharedkernel.domain.results;

public sealed interface Result<T> permits Result.Success, Result.Failure {

    boolean isSuccess();

    default boolean isFailure() {
        return !isSuccess();
    }

    T value();

    Error error();

    static <T> Result<T> success(T value) {
        return new Success<>(value);
    }

    static Result<Void> success() {
        return new Success<>(null);
    }

    static <T> Result<T> failure(Error error) {
        return new Failure<>(error);
    }

    record Success<T>(T value) implements Result<T> {

        @Override
        public boolean isSuccess() {
            return true;
        }

        @Override
        public Error error() {
            throw new IllegalStateException("Cannot access error of a successful result.");
        }
    }

    record Failure<T>(Error error) implements Result<T> {

        @Override
        public boolean isSuccess() {
            return false;
        }

        @Override
        public T value() {
            throw new IllegalStateException("Cannot access value of a failed result: " + error);
        }
    }
}
