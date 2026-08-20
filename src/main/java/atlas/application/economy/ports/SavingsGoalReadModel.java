package atlas.application.economy.ports;

import atlas.domain.economy.SavingsGoal;
import java.util.List;

public interface SavingsGoalReadModel {

    List<SavingsGoal> findAll();
}
