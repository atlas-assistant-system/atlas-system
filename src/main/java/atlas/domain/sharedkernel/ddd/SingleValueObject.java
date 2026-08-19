package atlas.domain.sharedkernel.ddd;

public interface SingleValueObject<T> extends ValueObject {

    T value();
}
