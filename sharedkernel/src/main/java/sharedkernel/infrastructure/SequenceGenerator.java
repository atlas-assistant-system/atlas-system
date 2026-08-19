package sharedkernel.infrastructure;

public interface SequenceGenerator {

    long next(String sequenceName);

    long current(String sequenceName);
}
