package atlas.app.economy;

import atlas.application.economy.commands.recordmovement.RecordMovementCommand;
import atlas.domain.economy.enums.Category;
import atlas.domain.economy.enums.MovementKind;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;

record Occasional(Category category, String note, long minCents, long maxCents) {

    private static final List<Occasional> ALL = List.of(
        new Occasional(Category.FOOD, "Supermercado", 3500, 9500),
        new Occasional(Category.FOOD, "Supermercado", 3500, 9500),
        new Occasional(Category.FOOD, "Comida fuera", 1200, 4500),
        new Occasional(Category.LEISURE, "Cine", 900, 2400),
        new Occasional(Category.LEISURE, "Libros", 1500, 4000),
        new Occasional(Category.TRANSPORT, "Guagua", 145, 800),
        new Occasional(Category.SHOPPING, "Ropa", 2500, 8000),
        new Occasional(Category.HEALTH, "Farmacia", 800, 3500),
        new Occasional(Category.OTHER, "Varios", 500, 3000));

    static RecordMovementCommand pick(Random random, LocalDate month) {
        var what = ALL.get(random.nextInt(ALL.size()));
        var cents = what.minCents() + random.nextLong(what.maxCents() - what.minCents());
        var day = 1 + random.nextInt(month.lengthOfMonth());

        return new RecordMovementCommand(
            MovementKind.EXPENSE, BigDecimal.valueOf(cents, 2), what.category(), what.note(),
            month.withDayOfMonth(day));
    }
}
