package atlas.application.economy.ports;

import atlas.application.sharedkernel.ports.Repository;
import atlas.domain.economy.SavingsGoal;
import atlas.domain.economy.SavingsGoalId;

public interface SavingsGoalRepository extends Repository<SavingsGoal, SavingsGoalId> {

    SavingsGoalId nextId();
}
