package atlas.infrastructure.sharedkernel;

public interface SequenceGenerator {

    long next(String sequenceName);

    long current(String sequenceName);
}
