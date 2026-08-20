package atlas.application.economy.ports;

import atlas.application.sharedkernel.ports.Repository;
import atlas.domain.economy.Budget;
import atlas.domain.economy.BudgetId;
import atlas.domain.economy.enums.Category;

public interface BudgetRepository extends Repository<Budget, BudgetId> {

    BudgetId nextId();

    boolean existsFor(Category category);
}
