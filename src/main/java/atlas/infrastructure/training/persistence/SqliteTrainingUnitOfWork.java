package atlas.infrastructure.training.persistence;

import atlas.application.sharedkernel.events.EventDelivery;
import atlas.application.sharedkernel.unitofwork.AbstractUnitOfWork;
import atlas.application.training.ports.ExerciseRepository;
import atlas.application.training.ports.TrainingUnitOfWork;
import atlas.application.training.ports.WorkoutLogRepository;
import atlas.application.training.ports.WorkoutRepository;
import atlas.infrastructure.sharedkernel.SequenceGenerator;
import atlas.infrastructure.sharedkernel.persistence.PersistenceException;
import java.sql.Connection;
import java.sql.SQLException;

public final class SqliteTrainingUnitOfWork extends AbstractUnitOfWork
    implements TrainingUnitOfWork {

    private final Connection connection;
    private final SqliteExerciseRepository exercises;
    private final SqliteWorkoutRepository workouts;
    private final SqliteWorkoutLogRepository logs;

    public SqliteTrainingUnitOfWork(
        Connection connection, EventDelivery delivery, SequenceGenerator sequences) {
        super(delivery);
        this.connection = connection;
        this.exercises = new SqliteExerciseRepository(connection, sequences);
        this.workouts = new SqliteWorkoutRepository(connection, sequences);
        this.logs = new SqliteWorkoutLogRepository(connection, sequences);
    }

    @Override
    public ExerciseRepository exercises() {
        return exercises;
    }

    @Override
    public WorkoutRepository workouts() {
        return workouts;
    }

    @Override
    public WorkoutLogRepository logs() {
        return logs;
    }

    @Override
    protected void begin() {
        try {
            connection.setAutoCommit(false);
        } catch (SQLException e) {
            throw new PersistenceException("Failed to begin a transaction", e);
        }
    }

    @Override
    protected void commit() {
        try {
            connection.commit();
        } catch (SQLException e) {
            throw new PersistenceException("Failed to commit the transaction", e);
        }
    }

    @Override
    protected void rollback() {
        try {
            connection.rollback();
        } catch (SQLException e) {
            throw new PersistenceException("Failed to roll back the transaction", e);
        }
    }
}
