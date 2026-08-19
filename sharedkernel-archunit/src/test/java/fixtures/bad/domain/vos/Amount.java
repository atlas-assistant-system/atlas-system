package fixtures.bad.domain.vos;

public final class Amount {

    private final long cents;

    public Amount(long cents) {
        this.cents = cents;
    }

    public long cents() {
        return cents;
    }
}
