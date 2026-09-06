package atlas.application.training.ports;

import java.util.UUID;


@FunctionalInterface
public interface IdGenerator {

    UUID next();
}
