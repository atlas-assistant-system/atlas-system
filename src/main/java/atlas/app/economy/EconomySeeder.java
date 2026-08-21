package atlas.app.economy;

import atlas.application.economy.commands.definebudget.DefineBudgetCommand;
import atlas.application.economy.commands.recordmovement.RecordMovementCommand;
import atlas.application.economy.commands.setsavingsgoal.SetSavingsGoalCommand;
import atlas.application.economy.queries.listmovements.ListMovementsQuery;
import atlas.application.sharedkernel.cqrs.CommandBus;
import atlas.application.sharedkernel.logging.LogEntryRenderer;
import atlas.domain.economy.enums.Category;
import atlas.domain.economy.enums.MovementKind;
import atlas.infrastructure.sharedkernel.logging.LogEntryRenderers;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class EconomySeeder {

    public static final int MONTHS_OF_HISTORY = 8;

    private static final int OCCASIONAL_PER_MONTH = 12;
    private static final long RANDOM_SEED = 20260821L;

    private static final List<Recurring> RECURRING = List.of(
        new Recurring(MovementKind.INCOME, Category.INCOME, "Nomina", 1, 210000, 0),
        new Recurring(MovementKind.EXPENSE, Category.HOME, "Alquiler", 3, 75000, 0),
        new Recurring(MovementKind.EXPENSE, Category.HOME, "Luz y agua", 12, 6500, 2500),
        new Recurring(MovementKind.EXPENSE, Category.HOME, "Internet y movil", 15, 4500, 0),
        new Recurring(MovementKind.EXPENSE, Category.TRANSPORT, "Gasolina", 8, 5500, 2000));

    public static final int MAX_MOVEMENTS = MONTHS_OF_HISTORY * (RECURRING.size() + OCCASIONAL_PER_MONTH);

    private EconomySeeder() {}

    public static void main(String[] args) {
        Logger.getLogger("sharedkernel.command").setLevel(Level.OFF);

        var directory = Path.of(args.length > 0 ? args[0] : "data");

        Summary summary;
        try {
            summary = seed(directory, Clock.systemDefaultZone());
        } catch (RuntimeException e) {
            if (!isDatabaseLocked(e)) {
                throw e;
            }

            System.err.println(lockedMessage(directory));
            System.exit(1);

            return;
        }

        System.out.println(summary.alreadyPopulated()
            ? "economy.db ya tiene movimientos: no se ha tocado nada."
            : summary.movements() + " movimientos, " + summary.budgets() + " presupuestos y "
                + summary.goals() + " objetivos en " + directory.resolve("economy.db"));
    }

    static boolean isDatabaseLocked(Throwable failure) {
        for (var cause = failure; cause != null; cause = cause.getCause()) {
            if (cause.getMessage() != null && cause.getMessage().contains("database is locked")) {
                return true;
            }
        }

        return false;
    }

    static String lockedMessage(Path directory) {
        return String.join(
            System.lineSeparator(),
            directory.resolve("economy.db") + " esta en uso por otro proceso.",
            "SQLite admite un solo escritor: para Atlas, siembra, y vuelve a arrancarlo.",
            "Si el seeder llego a escribir antes de fallar, borra economy.db y repite.");
    }

    public static Summary seed(Path dataDirectory, Clock clock) {
        var application = EconomyApplication.wire(quietRenderer(), dataDirectory, clock);
        try {
            if (!application.queries().dispatch(new ListMovementsQuery(null, null, null, 1)).value().isEmpty()) {
                return new Summary(0, 0, 0, true);
            }

            var today = LocalDate.now(clock);
            var movements = recordMovements(application.commands(), today);
            var budgets = defineBudgets(application.commands());
            var goals = setGoals(application.commands(), today);

            return new Summary(movements, budgets, goals, false);
        } finally {
            application.stop();
        }
    }

    private static int recordMovements(CommandBus commands, LocalDate today) {
        var random = new Random(RANDOM_SEED);
        var recorded = 0;

        for (var back = MONTHS_OF_HISTORY - 1; back >= 0; back--) {
            var month = today.withDayOfMonth(1).minusMonths(back);
            recorded += recordMonth(commands, random, month, today);
        }

        return recorded;
    }

    private static int recordMonth(CommandBus commands, Random random, LocalDate month, LocalDate today) {
        var recorded = 0;

        for (var recurring : RECURRING) {
            recorded += record(commands, recurring.on(month, random), today);
        }

        for (var extra = 0; extra < OCCASIONAL_PER_MONTH; extra++) {
            recorded += record(commands, Occasional.pick(random, month), today);
        }

        return recorded;
    }

    private static int record(CommandBus commands, RecordMovementCommand command, LocalDate today) {
        if (command.occurredOn().isAfter(today)) {
            return 0;
        }

        commands.dispatch(command);

        return 1;
    }

    private static int defineBudgets(CommandBus commands) {
        commands.dispatch(new DefineBudgetCommand(Category.FOOD, new BigDecimal("400.00")));
        commands.dispatch(new DefineBudgetCommand(Category.LEISURE, new BigDecimal("120.00")));
        commands.dispatch(new DefineBudgetCommand(Category.TRANSPORT, new BigDecimal("90.00")));

        return 3;
    }

    private static int setGoals(CommandBus commands, LocalDate today) {
        commands.dispatch(new SetSavingsGoalCommand(
            "Viaje a Japon", new BigDecimal("3000.00"), today.plusMonths(10).withDayOfMonth(1)));
        commands.dispatch(new SetSavingsGoalCommand(
            "Cambiar el portatil", new BigDecimal("1600.00"), today.plusMonths(5).withDayOfMonth(1)));

        return 2;
    }

    static LogEntryRenderer quietRenderer() {
        return LogEntryRenderers.forConsole(false, ZoneOffset.UTC);
    }

    public record Summary(int movements, int budgets, int goals, boolean alreadyPopulated) {}

    private record Recurring(
        MovementKind kind, Category category, String note, int dayOfMonth, long baseCents, long spreadCents) {

        RecordMovementCommand on(LocalDate month, Random random) {
            var cents = baseCents + (spreadCents == 0 ? 0 : random.nextLong(spreadCents));

            return new RecordMovementCommand(
                kind, euros(cents), category, note, month.withDayOfMonth(dayOfMonth));
        }
    }

    private record Occasional(Category category, String note, long minCents, long maxCents) {

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
                MovementKind.EXPENSE, euros(cents), what.category(), what.note(), month.withDayOfMonth(day));
        }
    }

    private static BigDecimal euros(long cents) {
        return BigDecimal.valueOf(cents, 2);
    }
}
