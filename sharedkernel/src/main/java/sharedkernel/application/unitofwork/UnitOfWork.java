package sharedkernel.application.unitofwork;

import java.util.function.Supplier;

public interface UnitOfWork {

    <T> T execute(Supplier<T> work);

    void run(Runnable work);
}
