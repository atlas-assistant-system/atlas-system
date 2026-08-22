package atlas.support.builders;

import atlas.infrastructure.sharedkernel.persistence.Migrations;
import atlas.infrastructure.sharedkernel.persistence.SchemaMigrator;
import atlas.infrastructure.sharedkernel.persistence.SqliteConnections;
import java.nio.file.Path;
import java.sql.Connection;
import java.time.Clock;

public final class NutritionTestDatabase {

    private NutritionTestDatabase() {}

    public static Connection open(Path directory, Clock clock) {
        var connection = SqliteConnections.openForContext(directory, "nutrition");
        new SchemaMigrator(connection, clock).migrate(Migrations.load(
            NutritionTestDatabase.class, "/db-migrations/nutrition",
            "V001__create_plans.sql", "V002__index_single_active_plan.sql",
            "V003__create_intakes.sql", "V004__index_intakes_by_date.sql",
            "V005__create_sequences.sql", "V006__create_weigh_ins.sql",
            "V007__add_calories_to_intakes.sql", "V008__backfill_intake_calories.sql",
            "V009__add_calories_to_plans.sql", "V010__backfill_plan_calories.sql"));

        return connection;
    }
}
