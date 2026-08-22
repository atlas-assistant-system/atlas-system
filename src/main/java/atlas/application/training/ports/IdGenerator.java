package atlas.application.training.ports;

import java.util.UUID;

/**
 * Los ids de las entidades internas. Es un puerto y no una llamada directa a
 * {@code UUID.randomUUID()} para que los tests puedan fijarlos, igual que el {@code Clock}.
 */
@FunctionalInterface
public interface IdGenerator {

    UUID next();
}
