package atlas.infrastructure.common;

import atlas.application.routines.ports.RoutineEntryIdGenerator;
import atlas.domain.routines.RoutineEntryId;
import java.util.UUID;

public final class UuidRoutineEntryIdGenerator implements RoutineEntryIdGenerator {

    @Override
    public RoutineEntryId next() {
        return RoutineEntryId.of(UUID.randomUUID());
    }
}
