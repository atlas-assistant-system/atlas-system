package sharedkernel.domain.ddd;

public interface SingleValueObject<T> extends ValueObject {

    T value();
}
