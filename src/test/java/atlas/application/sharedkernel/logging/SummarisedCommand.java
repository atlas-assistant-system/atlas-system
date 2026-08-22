package atlas.application.sharedkernel.logging;

import atlas.application.sharedkernel.cqrs.Command;
import atlas.domain.sharedkernel.results.Result;

record SummarisedCommand(String notes) implements Command<Result<String>>, LoggableSummary {

    @Override
    public String logSummary() {
        return "slot=2026-08-17T09:00";
    }
}
