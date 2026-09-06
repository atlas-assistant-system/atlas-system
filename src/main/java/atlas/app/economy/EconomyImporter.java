package atlas.app.economy;

import atlas.application.economy.queries.listmovements.ListMovementsQuery;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.util.logging.Level;
import java.util.logging.Logger;


public final class EconomyImporter {

    private EconomyImporter() {}

    public static void main(String[] args) throws IOException {
        Logger.getLogger("sharedkernel.command").setLevel(Level.OFF);

        if (args.length == 0) {
            System.err.println("Uso: importEconomy --args=\"<extracto.csv> [directorio-de-datos]\"");
            System.exit(1);

            return;
        }

        var csv = Path.of(args[0]);
        var directory = Path.of(args.length > 1 ? args[1] : EconomySettings.DEFAULT_DATA_DIRECTORY);

        ImportSummary summary;
        try {
            summary = importFrom(csv, directory, Clock.systemDefaultZone());
        } catch (RuntimeException e) {
            if (!EconomySeeder.isDatabaseLocked(e)) {
                throw e;
            }

            System.err.println(EconomySeeder.lockedMessage(directory));
            System.exit(1);

            return;
        }

        System.out.println(summary.alreadyPopulated()
            ? "economy.db ya tiene movimientos: no se ha tocado nada. Borralo si quieres reimportar."
            : summary.movements() + " movimientos de " + csv + " en " + directory.resolve("economy.db"));
    }

    public static ImportSummary importFrom(Path csv, Path dataDirectory, Clock clock) throws IOException {
        var commands = BankStatementDeserializer.deserialize(csv);

        var application = EconomyApplication.wire(EconomySeeder.quietRenderer(), dataDirectory, clock);
        try {
            if (!application.queries().dispatch(new ListMovementsQuery(null, null, null, 1)).value().isEmpty()) {
                return new ImportSummary(0, true);
            }

            commands.forEach(application.commands()::dispatch);

            return new ImportSummary(commands.size(), false);
        } finally {
            application.stop();
        }
    }
}
