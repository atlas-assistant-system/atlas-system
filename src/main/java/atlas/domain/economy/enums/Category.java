package atlas.domain.economy.enums;

public enum Category {

    FOOD("Comida", "🍽", MovementKind.EXPENSE),
    TRANSPORT("Transporte", "🚌", MovementKind.EXPENSE),
    HOME("Hogar", "🏠", MovementKind.EXPENSE),
    LEISURE("Ocio", "🎬", MovementKind.EXPENSE),
    HEALTH("Salud", "💊", MovementKind.EXPENSE),
    SHOPPING("Compras", "🛍", MovementKind.EXPENSE),
    OTHER("Otros", "•", MovementKind.EXPENSE),
    INCOME("Ingreso", "💶", MovementKind.INCOME);

    private final String label;
    private final String icon;
    private final MovementKind kind;

    Category(String label, String icon, MovementKind kind) {
        this.label = label;
        this.icon = icon;
        this.kind = kind;
    }

    public String label() {
        return label;
    }

    public String icon() {
        return icon;
    }

    public MovementKind kind() {
        return kind;
    }

    public boolean matches(MovementKind other) {
        return kind == other;
    }
}
