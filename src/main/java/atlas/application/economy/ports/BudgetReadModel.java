package atlas.application.economy.ports;

import atlas.domain.economy.Budget;
import java.util.List;

public interface BudgetReadModel {

    List<Budget> findAll();
}
