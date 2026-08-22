package atlas.support.builders;

import atlas.infrastructure.sharedkernel.persistence.Migrations;
import atlas.infrastructure.sharedkernel.persistence.SchemaMigrator;
import atlas.infrastructure.sharedkernel.persistence.SqliteConnections;
import java.nio.file.Path;
import java.sql.Connection;
import java.time.Clock;

public final class TrainingTestDatabase {

    private TrainingTestDatabase() {}

    public static Connection open(Path directory, Clock clock) {
        var connection = SqliteConnections.openForContext(directory, "training");
        new SchemaMigrator(connection, clock).migrate(Migrations.load(
            TrainingTestDatabase.class, "/db-migrations/training",
            "V001__create_exercises.sql", "V002__index_exercises_by_name.sql",
            "V003__create_workouts.sql", "V004__create_workout_exercises.sql",
            "V005__create_workout_logs.sql", "V006__create_set_logs.sql",
            "V007__index_set_logs_by_exercise.sql", "V008__index_workout_logs_by_date.sql",
            "V009__create_sequences.sql", "V010__add_workout_days.sql"));

        return connection;
    }
}
