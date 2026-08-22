package atlas.application.sharedkernel.logging;

import atlas.application.sharedkernel.cqrs.Command;
import atlas.domain.sharedkernel.results.Result;

record ScheduleAppointment(String slot, String notes) implements Command<Result<String>> {}
