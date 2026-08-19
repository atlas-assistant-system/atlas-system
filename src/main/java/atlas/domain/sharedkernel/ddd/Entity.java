package atlas.domain.sharedkernel.ddd;

import atlas.domain.sharedkernel.guards.ObjectGuard;

public abstract class Entity<TId> {

    private final TId id;

    protected Entity(TId id) {
        this.id = ObjectGuard.notNull(id, "id");
    }

    public TId id() {
        return id;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        return id.equals(((Entity<?>) other).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
