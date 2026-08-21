package atlas.application.nutrition.ports;

import atlas.application.sharedkernel.unitofwork.UnitOfWork;

public interface NutritionUnitOfWork extends UnitOfWork {

    PlanRepository plans();

    IntakeRepository intakes();

    WeighInRepository weighIns();
}
