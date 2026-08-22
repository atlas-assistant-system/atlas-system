package atlas.application.sharedkernel.logging;

import atlas.application.sharedkernel.cqrs.Command;
import atlas.domain.sharedkernel.results.Result;

record NoisyCommand() implements Command<Result<String>>, LoggableSummary {

    @Override
    public String logSummary() {
        return "first\nINFO: forged line\tend";
    }
}
